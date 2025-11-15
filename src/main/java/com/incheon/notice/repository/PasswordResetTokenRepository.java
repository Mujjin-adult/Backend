package com.incheon.notice.repository;

import com.incheon.notice.entity.PasswordResetToken;
import com.incheon.notice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 비밀번호 재설정 토큰 Repository
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * 토큰으로 조회
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * 사용자로 조회 (가장 최근 토큰)
     */
    Optional<PasswordResetToken> findTopByUserOrderByExpiryDateDesc(User user);

    /**
     * 사용자 ID로 조회
     */
    List<PasswordResetToken> findByUserId(Long userId);

    /**
     * 만료된 토큰 삭제 (정리용)
     */
    void deleteByExpiryDateBefore(LocalDateTime dateTime);

    /**
     * 사용된 토큰 삭제 (정리용)
     */
    void deleteByUsedTrue();
}
