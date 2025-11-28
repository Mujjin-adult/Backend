package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 키워드 엔티티
 * 사용자가 등록한 키워드와 일치하는 공지사항이 새로 등록되면 푸시 알림 발송
 */
@Entity
@Table(name = "notification_keyword",
        indexes = {
                @Index(name = "idx_notification_keyword_user_id", columnList = "user_id"),
                @Index(name = "idx_notification_keyword_keyword", columnList = "keyword"),
                @Index(name = "idx_notification_keyword_is_active", columnList = "is_active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_keyword", columnNames = {"user_id", "keyword"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 (다대일 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 알림 키워드
     * 예: "장학금", "학사 일정", "취업" 등
     */
    @Column(name = "keyword", nullable = false, length = 50)
    private String keyword;

    /**
     * 카테고리 ID 필터 (선택사항)
     * null이면 모든 카테고리에서 검색
     */
    @Column(name = "category_id")
    private Long categoryId;

    /**
     * 알림 활성화 여부
     * true: 알림 켜짐, false: 알림 꺼짐 (일시 정지)
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 매칭된 공지사항 수
     * 통계용: 이 키워드로 몇 개의 공지사항과 매칭되었는지
     */
    @Column(name = "matched_count", nullable = false)
    @Builder.Default
    private Integer matchedCount = 0;

    /**
     * 마지막 알림 발송 시각
     * 중복 알림 방지용
     */
    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    /**
     * 생성일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 생성 시 자동으로 현재 시각 설정
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 수정 시 자동으로 현재 시각 설정
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 알림 활성화/비활성화 토글
     */
    public void toggleActive() {
        this.isActive = !this.isActive;
    }

    /**
     * 매칭 카운트 증가
     */
    public void incrementMatchedCount() {
        this.matchedCount++;
    }

    /**
     * 마지막 알림 발송 시각 업데이트
     */
    public void updateLastNotifiedAt() {
        this.lastNotifiedAt = LocalDateTime.now();
    }

    /**
     * 키워드 업데이트
     */
    public void updateKeyword(String newKeyword) {
        this.keyword = newKeyword;
    }

    /**
     * 카테고리 필터 업데이트
     */
    public void updateCategoryId(Long newCategoryId) {
        this.categoryId = newCategoryId;
    }
}
