package com.incheon.notice.service;

import com.incheon.notice.entity.EmailVerificationToken;
import com.incheon.notice.entity.User;
import com.incheon.notice.repository.EmailVerificationTokenRepository;
import com.incheon.notice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이메일 인증 서비스
 * 이메일 인증 토큰 생성 및 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final int TOKEN_EXPIRY_HOURS = 24;

    /**
     * 이메일 인증 토큰 생성 및 메일 발송
     */
    @Transactional
    public void createAndSendVerificationToken(User user) {
        log.debug("이메일 인증 토큰 생성: userId={}", user.getId());

        // 기존 토큰 삭제 (사용되지 않은 토큰)
        tokenRepository.findByUserId(user.getId()).forEach(tokenRepository::delete);

        // 새 토큰 생성
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .used(false)
                .build();

        tokenRepository.save(verificationToken);

        // 인증 메일 발송 (실패해도 회원가입은 성공)
        try {
            emailService.sendVerificationEmail(user.getEmail(), token);
            log.info("이메일 인증 토큰 생성 및 발송 완료: userId={}, email={}", user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("이메일 발송 실패 (회원가입은 성공): userId={}, email={}, error={}",
                user.getId(), user.getEmail(), e.getMessage());
        }
    }

    /**
     * 이메일 인증 처리
     */
    @Transactional
    public void verifyEmail(String token) {
        log.debug("이메일 인증 처리: token={}", token);

        // 토큰 조회
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 인증 토큰입니다"));

        // 토큰 유효성 검증
        if (!verificationToken.isValid()) {
            if (verificationToken.isExpired()) {
                throw new RuntimeException("인증 토큰이 만료되었습니다. 새로운 인증 메일을 요청해주세요");
            }
            if (verificationToken.getUsed()) {
                throw new RuntimeException("이미 사용된 인증 토큰입니다");
            }
        }

        // 사용자 이메일 인증 완료 처리
        User user = verificationToken.getUser();
        user.verifyEmail();

        // 토큰 사용 처리
        verificationToken.markAsUsed();

        log.info("이메일 인증 완료: userId={}, email={}", user.getId(), user.getEmail());
    }

    /**
     * 인증 메일 재발송
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        log.debug("인증 메일 재발송: email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 이미 인증된 경우
        if (user.getIsEmailVerified()) {
            throw new RuntimeException("이미 인증된 이메일입니다");
        }

        // 새 토큰 생성 및 메일 발송
        createAndSendVerificationToken(user);
    }
}
