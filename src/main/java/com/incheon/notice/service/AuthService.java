package com.incheon.notice.service;

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
                            .role(user.getRole().name())
                            .build())
                    .build();
        } catch (FirebaseAuthException e) {
            log.error("Firebase authentication failed: {}", e.getMessage());
            throw new BusinessException("인증에 실패했습니다: " + e.getMessage());
        }
    }
}
