package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 알림 설정 엔티티
 * 사용자가 구독한 카테고리 정보 (해당 카테고리의 새 공지사항이 올라오면 푸시 알림)
 */
@Entity
@Table(name = "user_preferences",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_category", columnNames = {"user_id", "category_id"})
    },
    indexes = {
        @Index(name = "idx_user_pref_user_id", columnList = "user_id"),
        @Index(name = "idx_user_pref_category_id", columnList = "category_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;  // 구독한 카테고리

    @Column(nullable = false)
    @Builder.Default
    private Boolean notificationEnabled = true;  // 푸시 알림 활성화 여부

    /**
     * 알림 설정 변경
     */
    public void toggleNotification(Boolean enabled) {
        this.notificationEnabled = enabled;
    }
}
