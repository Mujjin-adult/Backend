package com.incheon.notice.service;

import com.incheon.notice.entity.PasswordResetToken;
import com.incheon.notice.entity.User;
import com.incheon.notice.repository.PasswordResetTokenRepository;
import com.incheon.notice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 비밀번호 재설정 서비스
 * 비밀번호 찾기 및 재설정 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int TOKEN_EXPIRY_HOURS = 1;  // 비밀번호 재설정 토큰은 1시간만 유효

    /**
     * 비밀번호 재설정 토큰 생성 및 메일 발송
     */
    @Transactional
    public void createAndSendResetToken(String email) {
        log.debug("비밀번호 재설정 토큰 생성: email={}", email);

        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일로 등록된 사용자를 찾을 수 없습니다"));

        // 기존 토큰 삭제 (사용되지 않은 토큰)
        tokenRepository.findByUserId(user.getId()).forEach(tokenRepository::delete);

        // 새 토큰 생성
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        // 재설정 메일 발송
        emailService.sendPasswordResetEmail(user.getEmail(), token);

        log.info("비밀번호 재설정 토큰 생성 및 발송 완료: email={}", user.getEmail());
    }

    /**
     * 비밀번호 재설정 처리
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.debug("비밀번호 재설정 처리: token={}", token);

        // 토큰 조회
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 비밀번호 재설정 토큰입니다"));

        // 토큰 유효성 검증
        if (!resetToken.isValid()) {
            if (resetToken.isExpired()) {
                throw new RuntimeException("비밀번호 재설정 토큰이 만료되었습니다. 새로운 재설정을 요청해주세요");
            }
            if (resetToken.getUsed()) {
                throw new RuntimeException("이미 사용된 비밀번호 재설정 토큰입니다");
            }
        }

        // 비밀번호 변경
        User user = resetToken.getUser();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);

        // 토큰 사용 처리
        resetToken.markAsUsed();

        log.info("비밀번호 재설정 완료: userId={}, email={}", user.getId(), user.getEmail());
    }

    /**
     * 토큰 유효성 확인
     */
    public boolean isTokenValid(String token) {
        return tokenRepository.findByToken(token)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }
}
