package com.incheon.notice.service;

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
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;

    /**
     * 회원가입 (레거시)
     *
     * ⚠️ 주의: 이 메서드는 하위 호환성을 위해 유지됩니다.
     * Firebase Authentication 사용을 권장합니다.
     *
     * Firebase 권장 플로우:
     * 1. 클라이언트: Firebase SDK로 회원가입 (createUserWithEmailAndPassword)
     * 2. 클라이언트: Firebase ID Token 발급 (getIdToken)
     * 3. 클라이언트: login() API 호출 → 서버 DB에 자동 생성
     *
     * 이 메서드는 Firebase를 사용하지 않는 특수한 경우에만 사용하세요.
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

        // 사용자 생성
        User user = User.builder()
                .studentId(request.getStudentId())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(UserRole.USER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        // 이메일 인증 토큰 생성 및 메일 발송
        try {
            emailVerificationService.createAndSendVerificationToken(savedUser);
        } catch (Exception e) {
            // 이메일 발송 실패 시 로그만 남기고 회원가입은 성공 처리
            // 사용자가 나중에 인증 메일을 재요청할 수 있음
            log.error("이메일 인증 메일 발송 실패: email={}, error={}", savedUser.getEmail(), e.getMessage());
        }

        return AuthDto.UserResponse.builder()
                .id(savedUser.getId())
                .studentId(savedUser.getStudentId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(savedUser.getRole().name())
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

            // 자체 DB에서 사용자 조회 (없으면 자동 회원가입)
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        // Firebase로 로그인했지만 자체 DB에 없는 경우 자동 생성
                        User newUser = User.builder()
                                .email(email)
                                .name(firebaseToken.getName() != null ? firebaseToken.getName() : "사용자")
                                .studentId(firebaseUid) // Firebase UID를 학번 대신 사용 (임시)
                                .password(passwordEncoder.encode(firebaseUid)) // 임시 비밀번호
                                .role(UserRole.USER)
                                .isActive(true)
                                .isEmailVerified(firebaseToken.isEmailVerified())
                                .build();
                        return userRepository.save(newUser);
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
}
