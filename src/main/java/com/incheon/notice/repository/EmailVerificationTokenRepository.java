package com.incheon.notice.repository;

import com.incheon.notice.entity.EmailVerificationToken;
import com.incheon.notice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 이메일 인증 토큰 Repository
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * 토큰으로 조회
     */
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * 사용자로 조회 (가장 최근 토큰)
     */
    Optional<EmailVerificationToken> findTopByUserOrderByExpiryDateDesc(User user);

    /**
     * 사용자 ID로 조회
     */
    List<EmailVerificationToken> findByUserId(Long userId);

    /**
     * 만료된 토큰 삭제 (정리용)
     */
    void deleteByExpiryDateBefore(LocalDateTime dateTime);

    /**
     * 사용된 토큰 삭제 (정리용)
     */
    void deleteByUsedTrue();
}
