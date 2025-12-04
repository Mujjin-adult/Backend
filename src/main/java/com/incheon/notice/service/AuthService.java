package com.incheon.notice.service;

import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.incheon.notice.dto.AuthDto;
import com.incheon.notice.entity.User;
import com.incheon.notice.entity.UserRole;
import com.incheon.notice.exception.BusinessException;
import com.incheon.notice.exception.DuplicateResourceException;
import com.incheon.notice.repository.UserRepository;
import com.incheon.notice.security.FirebaseTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스
 * 회원가입, 로그인, 토큰 갱신 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FirebaseTokenProvider firebaseTokenProvider;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * 회원가입 (Firebase 통합)
     *
     * 서버에서 Firebase Authentication에 사용자를 생성하고 DB에 저장합니다.
     *
     * 플로우:
     * 1. 이메일/학번 중복 체크
     * 2. Firebase Authentication에 사용자 생성
     * 3. DB에 사용자 저장 (Firebase UID 포함)
     * 4. 이메일 인증 링크 발송
     * 5. 성공 응답 (클라이언트는 이메일 인증 후 로그인)
     *
     * ⚠️ 중요:
     * - idToken은 클라이언트에서 로그인 후 발급받아야 합니다
     * - fcmToken도 클라이언트 디바이스에서 발급받아야 합니다
     * - 회원가입 후 반드시 login() API를 호출하여 토큰을 등록하세요
     */
    @Transactional
    public AuthDto.UserResponse signUp(AuthDto.SignUpRequest request) {
        // 이메일 도메인 검증 (inu.ac.kr)
        if (!request.getEmail().endsWith("@inu.ac.kr")) {
            throw new BusinessException("인천대학교 이메일(@inu.ac.kr)만 사용 가능합니다");
        }

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("이미 사용중인 이메일입니다");
        }

        // 학번 중복 체크
        if (userRepository.existsByStudentId(request.getStudentId())) {
            throw new DuplicateResourceException("이미 사용중인 학번입니다");
        }

        String firebaseUid = null;

        try {
            // Firebase에 이미 존재하는지 확인
            try {
                var existingUser = FirebaseAuth.getInstance().getUserByEmail(request.getEmail());
                firebaseUid = existingUser.getUid();
                log.info("Firebase에 이미 존재하는 사용자: email={}, uid={}", request.getEmail(), firebaseUid);
            } catch (FirebaseAuthException e) {
                if (e.getAuthErrorCode().name().equals("USER_NOT_FOUND")) {
                    // Firebase에 사용자 생성
                    var userRecord = FirebaseAuth.getInstance()
                            .createUser(new com.google.firebase.auth.UserRecord.CreateRequest()
                                    .setEmail(request.getEmail())
                                    .setPassword(request.getPassword())
                                    .setDisplayName(request.getName())
                                    .setEmailVerified(false));
                    firebaseUid = userRecord.getUid();
                    log.info("Firebase 사용자 생성 완료: email={}, uid={}", request.getEmail(), firebaseUid);
                } else {
                    throw e;
                }
            }
        } catch (FirebaseAuthException e) {
            log.error("Firebase 사용자 생성 실패: email={}, error={}", request.getEmail(), e.getMessage());
            throw new BusinessException("회원가입에 실패했습니다: " + e.getMessage());
        }

        // DB에 사용자 저장 (Firebase UID 포함)
        User user = User.builder()
                .firebaseUid(firebaseUid)
                .studentId(request.getStudentId())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .department(request.getDepartment())
                .role(UserRole.USER)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        log.info("회원가입 완료: email={}, firebaseUid={}", savedUser.getEmail(), firebaseUid);

        return AuthDto.UserResponse.builder()
                .id(savedUser.getId())
                .studentId(savedUser.getStudentId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .department(savedUser.getDepartment())
                .role(savedUser.getRole().name())
                .build();
    }

    /**
     * 이메일/비밀번호 로그인 (서버 인증 + Firebase 커스텀 토큰 발급)
     *
     * 서버에서 이메일/비밀번호를 검증하고 Firebase 커스텀 토큰을 발급합니다.
     *
     * 플로우:
     * 1. 이메일/비밀번호 검증 (DB)
     * 2. Firebase 커스텀 토큰 생성
     * 3. 사용자 정보와 커스텀 토큰 반환
     * 4. 클라이언트: 커스텀 토큰으로 Firebase 로그인 (선택사항)
     *
     * @param request 이메일/비밀번호
     * @return 로그인 응답 (커스텀 토큰 포함)
     */
    @Transactional
    public AuthDto.LoginResponse loginWithEmail(AuthDto.EmailLoginRequest request) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다"));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        // 3. 계정 활성화 확인
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException("비활성화된 계정입니다. 관리자에게 문의하세요.");
        }

        // 4. FCM 토큰 업데이트 (있는 경우)
        if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
            user.updateFcmToken(request.getFcmToken());
            userRepository.save(user);
        }

        String customToken = null;
        String firebaseUid = user.getFirebaseUid();

        // 5. Firebase UID가 없는 경우 자동 생성
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            try {
                // Firebase에 사용자 생성
                var userRecord = FirebaseAuth.getInstance()
                        .createUser(new com.google.firebase.auth.UserRecord.CreateRequest()
                                .setEmail(user.getEmail())
                                .setPassword(request.getPassword())
                                .setDisplayName(user.getName())
                                .setEmailVerified(false));
                firebaseUid = userRecord.getUid();
                user.updateFirebaseUid(firebaseUid);
                userRepository.save(user);
                log.info("로그인 시 Firebase 사용자 자동 생성: email={}, uid={}", user.getEmail(), firebaseUid);
            } catch (FirebaseAuthException e) {
                // Firebase에 이미 존재하는 경우
                if (e.getAuthErrorCode().name().equals("EMAIL_ALREADY_EXISTS")) {
                    try {
                        var existingUser = FirebaseAuth.getInstance().getUserByEmail(user.getEmail());
                        firebaseUid = existingUser.getUid();
                        user.updateFirebaseUid(firebaseUid);
                        userRepository.save(user);
                        log.info("로그인 시 기존 Firebase UID 연결: email={}, uid={}", user.getEmail(), firebaseUid);
                    } catch (FirebaseAuthException ex) {
                        log.error("Firebase 사용자 조회 실패: email={}, error={}", user.getEmail(), ex.getMessage());
                    }
                } else {
                    log.error("Firebase 사용자 생성 실패: email={}, error={}", user.getEmail(), e.getMessage());
                }
            }
        }

        // 6. Firebase 커스텀 토큰 생성 (Firebase UID가 있는 경우)
        if (firebaseUid != null && !firebaseUid.isEmpty()) {
            try {
                customToken = FirebaseAuth.getInstance().createCustomToken(firebaseUid);
                log.info("Firebase 커스텀 토큰 생성 완료: email={}, uid={}", user.getEmail(), firebaseUid);
            } catch (FirebaseAuthException e) {
                log.error("Firebase 커스텀 토큰 생성 실패: email={}, uid={}, error={}", user.getEmail(), firebaseUid, e.getMessage());
                // 커스텀 토큰 생성 실패 시 Firebase UID가 유효하지 않을 수 있으므로 null로 초기화
                if (e.getAuthErrorCode().name().equals("USER_NOT_FOUND")) {
                    user.updateFirebaseUid(null);
                    userRepository.save(user);
                    log.warn("유효하지 않은 Firebase UID 제거: email={}, invalidUid={}", user.getEmail(), firebaseUid);
                }
            }
        }

        // 7. 응답 생성
        return AuthDto.LoginResponse.builder()
                .idToken(customToken)  // Firebase 커스텀 토큰 (클라이언트에서 Firebase 로그인 시 사용)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(AuthDto.UserResponse.builder()
                        .id(user.getId())
                        .studentId(user.getStudentId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .department(user.getDepartment())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    /**
     * Firebase ID Token으로 로그인 (Firebase Authentication)
     *
     * Firebase SDK로 로그인한 후 발급받은 ID Token을 검증하고 사용자 정보를 동기화합니다.
     *
     * 플로우:
     * 1. 클라이언트: Firebase SDK로 로그인 (signInWithEmailAndPassword, signInWithPopup 등)
     * 2. 클라이언트: Firebase ID Token 발급 (user.getIdToken())
     * 3. 클라이언트: 이 API에 ID Token 전송
     * 4. 서버: Firebase Admin SDK로 ID Token 검증
     * 5. 서버: 사용자 정보 조회/생성 (없으면 자동 회원가입)
     * 6. 서버: FCM 토큰 업데이트 (선택사항)
     *
     * 자동 회원가입: Firebase로 인증된 사용자가 서버 DB에 없는 경우 자동으로 생성됩니다.
     *
     * @param request Firebase ID Token과 선택적 FCM Token
     * @return 로그인 응답 (ID Token, 사용자 정보)
     * @throws BusinessException Firebase 인증 실패 시
     */
    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        try {
            // Firebase ID Token 검증
            FirebaseToken firebaseToken = firebaseTokenProvider.verifyToken(request.getIdToken());
            String email = firebaseToken.getEmail();
            String firebaseUid = firebaseToken.getUid();

            // 1. Firebase UID로 사용자 조회 (우선순위 1)
            User user = userRepository.findByFirebaseUid(firebaseUid)
                    .orElseGet(() -> {
                        // 2. 이메일로 사용자 조회 (기존 사용자 지원)
                        return userRepository.findByEmail(email)
                                .map(existingUser -> {
                                    // 기존 사용자에게 Firebase UID 설정
                                    if (existingUser.getFirebaseUid() == null) {
                                        existingUser.updateFirebaseUid(firebaseUid);
                                        log.info("기존 사용자에게 Firebase UID 연결: email={}, uid={}", email, firebaseUid);
                                    }
                                    return existingUser;
                                })
                                .orElseGet(() -> {
                                    // 3. 완전히 새로운 사용자 - 자동 회원가입
                                    User newUser = User.builder()
                                            .firebaseUid(firebaseUid)
                                            .email(email)
                                            .name(firebaseToken.getName() != null ? firebaseToken.getName() : "사용자")
                                            .studentId(null) // 학번은 나중에 클라이언트에서 입력
                                            .password(passwordEncoder.encode(firebaseUid)) // 임시 비밀번호
                                            .role(UserRole.USER)
                                            .isActive(true)
                                            .isEmailVerified(firebaseToken.isEmailVerified())
                                            .build();
                                    log.info("Firebase 자동 회원가입: email={}, uid={}", email, firebaseUid);
                                    return userRepository.save(newUser);
                                });
                    });

            // FCM 토큰 업데이트 (있는 경우)
            if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
                user.updateFcmToken(request.getFcmToken());
            }

            return AuthDto.LoginResponse.builder()
                    .idToken(request.getIdToken()) // Firebase ID Token을 그대로 반환
                    .tokenType("Bearer")
                    .expiresIn(3600L) // Firebase ID Token은 보통 1시간 유효
                    .user(AuthDto.UserResponse.builder()
                            .id(user.getId())
                            .studentId(user.getStudentId())
                            .email(user.getEmail())
                            .name(user.getName())
                            .department(user.getDepartment())
                            .role(user.getRole().name())
                            .build())
                    .build();
        } catch (FirebaseAuthException e) {
            log.error("Firebase authentication failed: {}", e.getMessage());
            throw new BusinessException("인증에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * Firebase UID로 로그인 (테스트/개발용)
     *
     * Firebase UID를 직접 사용하여 로그인합니다.
     * Firebase 콘솔에서 확인한 UID를 사용할 수 있습니다.
     *
     * @param request Firebase UID와 선택적 FCM Token
     * @return 로그인 응답 (커스텀 토큰, 사용자 정보)
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public AuthDto.LoginResponse loginWithFirebaseUid(AuthDto.FirebaseUidLoginRequest request) {
        String firebaseUid = request.getFirebaseUid();
        log.info("Firebase UID 로그인 시도: uid={}", firebaseUid);

        // 1. Firebase UID로 사용자 조회
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> {
                    log.warn("Firebase UID로 사용자를 찾을 수 없음: uid={}", firebaseUid);
                    return new BusinessException("등록되지 않은 Firebase UID입니다. 먼저 회원가입을 해주세요.");
                });

        // 2. 계정 활성화 확인
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException("비활성화된 계정입니다. 관리자에게 문의하세요.");
        }

        // 3. FCM 토큰 업데이트 (있는 경우)
        if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
            user.updateFcmToken(request.getFcmToken());
            userRepository.save(user);
        }

        // 4. Firebase 커스텀 토큰 생성
        String customToken = null;
        try {
            customToken = FirebaseAuth.getInstance().createCustomToken(firebaseUid);
            log.info("Firebase 커스텀 토큰 생성 완료: email={}, uid={}", user.getEmail(), firebaseUid);
        } catch (FirebaseAuthException e) {
            log.error("Firebase 커스텀 토큰 생성 실패: uid={}, error={}", firebaseUid, e.getMessage());
            throw new BusinessException("토큰 생성에 실패했습니다: " + e.getMessage());
        }

        // 5. 응답 생성
        return AuthDto.LoginResponse.builder()
                .idToken(customToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(AuthDto.UserResponse.builder()
                        .id(user.getId())
                        .studentId(user.getStudentId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .department(user.getDepartment())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    /**
     * 아이디 찾기 (이름, 학번으로 이메일 찾기)
     * 마스킹된 이메일 반환 및 전체 이메일 메일로 발송
     */
    @Transactional(readOnly = true)
    public AuthDto.FindIdResponse findId(AuthDto.FindIdRequest request) {
        // 이름과 학번으로 사용자 조회
        User user = userRepository.findByNameAndStudentId(request.getName(), request.getStudentId())
                .orElseThrow(() -> new BusinessException("일치하는 사용자 정보를 찾을 수 없습니다"));

        String email = user.getEmail();

        // 이메일 마스킹 (예: chosunghoon@inu.ac.kr → ch***@inu.ac.kr)
        String maskedEmail = maskEmail(email);

        // 이메일로 전체 이메일 주소 발송
        try {
            emailService.sendFindIdEmail(email);
            log.info("아이디 찾기 이메일 발송 완료: email={}", email);
        } catch (Exception e) {
            log.error("아이디 찾기 이메일 발송 실패: email={}, error={}", email, e.getMessage());
            throw new BusinessException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        return AuthDto.FindIdResponse.builder()
                .maskedEmail(maskedEmail)
                .message("입력하신 이메일 주소로 아이디가 전송되었습니다")
                .build();
    }

    /**
     * 이메일 마스킹 처리
     * 예: chosunghoon@inu.ac.kr → ch***@inu.ac.kr
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String localPart = parts[0];  // @  앞부분
        String domain = parts[1];     // @ 뒤부분

        // 로컬 부분 마스킹 (앞 2자리만 표시, 나머지 ***)
        String masked;
        if (localPart.length() <= 2) {
            masked = localPart + "***";
        } else {
            masked = localPart.substring(0, 2) + "***";
        }

        return masked + "@" + domain;
    }

    /**
     * 이메일 인증 링크 생성 및 발송 (Firebase)
     *
     * 서버에서 Firebase Admin SDK를 사용하여 이메일 인증 링크를 생성하고 발송합니다.
     *
     * ⚠️ 권장: 클라이언트에서 Firebase SDK의 sendEmailVerification()을 사용하는 것이 더 간단합니다.
     *
     * 이 메서드는 다음과 같은 경우에 사용하세요:
     * - 커스텀 이메일 템플릿이 필요한 경우
     * - 서버에서 이메일 발송을 완전히 제어해야 하는 경우
     *
     * @param email 이메일 주소
     * @return 성공 메시지
     * @throws BusinessException 이메일 발송 실패 시
     */
    @Transactional(readOnly = true)
    public String sendEmailVerification(String email) {
        try {
            // 1. Firebase에서 사용자 조회
            var firebaseUser = FirebaseAuth.getInstance().getUserByEmail(email);

            // 2. 이미 인증된 경우
            if (firebaseUser.isEmailVerified()) {
                log.info("이미 인증된 이메일: email={}", email);
                return "이미 인증된 이메일입니다";
            }

            // 3. ActionCodeSettings 생성 (인증 후 리다이렉트 URL 설정)
            ActionCodeSettings actionCodeSettings = ActionCodeSettings.builder()
                    .setUrl(frontendUrl + "/email-verified") // 인증 완료 후 이동할 URL
                    .setHandleCodeInApp(false) // 이메일 링크를 앱에서 처리하지 않음
                    .build();

            // 4. 이메일 인증 링크 생성
            String verificationLink = FirebaseAuth.getInstance()
                    .generateEmailVerificationLink(email, actionCodeSettings);

            // 5. 이메일 발송 (EmailService 사용)
            emailService.sendFirebaseVerificationEmail(email, verificationLink);

            log.info("이메일 인증 링크 발송 완료: email={}", email);
            return "이메일 인증 링크가 발송되었습니다";

        } catch (FirebaseAuthException e) {
            log.error("이메일 인증 링크 생성 실패: email={}, error={}", email, e.getMessage());
            throw new BusinessException("이메일 인증 링크 생성에 실패했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("이메일 발송 실패: email={}, error={}", email, e.getMessage());
            throw new BusinessException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 이메일 인증 메일 재발송 (Firebase)
     *
     * @param email 이메일 주소
     * @return 성공 메시지
     * @throws BusinessException 이메일 발송 실패 시
     */
    @Transactional(readOnly = true)
    public String resendEmailVerification(String email) {
        // 사용자가 DB에 존재하는지 확인
        userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("등록되지 않은 이메일입니다"));

        return sendEmailVerification(email);
    }

    /**
     * 비밀번호 재설정 이메일 발송 (Firebase)
     *
     * Firebase Admin SDK를 사용하여 비밀번호 재설정 링크를 생성하고 이메일로 발송합니다.
     *
     * 플로우:
     * 1. 사용자가 이메일 입력
     * 2. Firebase에서 비밀번호 재설정 링크 생성
     * 3. 이메일로 재설정 링크 발송
     * 4. 사용자가 링크 클릭 → Firebase 호스팅 페이지에서 새 비밀번호 입력
     * 5. Firebase에서 자동으로 비밀번호 업데이트
     *
     * @param email 비밀번호를 재설정할 이메일 주소
     * @return 성공 메시지
     * @throws BusinessException 이메일 발송 실패 시
     */
    @Transactional(readOnly = true)
    public String sendPasswordResetEmail(String email) {
        try {
            // 1. 사용자가 DB에 존재하는지 확인
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException("등록되지 않은 이메일입니다"));

            // 2. Firebase에서 사용자 조회
            try {
                FirebaseAuth.getInstance().getUserByEmail(email);
            } catch (FirebaseAuthException e) {
                if (e.getAuthErrorCode().name().equals("USER_NOT_FOUND")) {
                    log.warn("Firebase에 사용자 없음. DB에만 존재: email={}", email);
                    throw new BusinessException("Firebase 인증을 사용하는 계정이 아닙니다");
                }
                throw e;
            }

            // 3. ActionCodeSettings 생성 (비밀번호 재설정 후 리다이렉트 URL)
            ActionCodeSettings actionCodeSettings = ActionCodeSettings.builder()
                    .setUrl(frontendUrl + "/reset-password-complete")
                    .setHandleCodeInApp(false)
                    .build();

            // 4. 비밀번호 재설정 링크 생성
            String resetLink = FirebaseAuth.getInstance()
                    .generatePasswordResetLink(email, actionCodeSettings);

            // 5. 이메일 발송
            emailService.sendFirebasePasswordResetEmail(email, user.getName(), resetLink);

            log.info("비밀번호 재설정 이메일 발송 완료: email={}", email);
            return "비밀번호 재설정 이메일이 발송되었습니다. 이메일을 확인해주세요.";

        } catch (FirebaseAuthException e) {
            log.error("비밀번호 재설정 링크 생성 실패: email={}, error={}", email, e.getMessage());
            throw new BusinessException("비밀번호 재설정 링크 생성에 실패했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("비밀번호 재설정 이메일 발송 실패: email={}, error={}", email, e.getMessage());
            throw new BusinessException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
