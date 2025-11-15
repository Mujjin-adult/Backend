package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 이메일 인증 토큰 엔티티
 * 회원가입 시 이메일 인증을 위한 토큰
 */
@Entity
@Table(name = "email_verification_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;  // 인증 토큰

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 사용자

    @Column(nullable = false)
    private LocalDateTime expiryDate;  // 만료 시간

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;  // 사용 여부

    @Column
    private LocalDateTime verifiedAt;  // 인증 완료 시간

    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * 토큰을 사용 처리
     */
    public void markAsUsed() {
        this.used = true;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * 토큰이 유효한지 확인 (만료되지 않고 사용되지 않은 경우)
     */
    public boolean isValid() {
        return !isExpired() && !used;
    }
}
