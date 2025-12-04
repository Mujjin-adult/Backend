package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 상세 카테고리 구독 엔티티 (user_detail_category_subscriptions 테이블)
 * 사용자가 어떤 상세 카테고리를 구독했는지 관리
 */
@Entity
@Table(name = "user_detail_category_subscriptions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_detail_category", columnNames = {"user_id", "detail_category_id"})
    },
    indexes = {
        @Index(name = "idx_udcs_user_id", columnList = "user_id"),
        @Index(name = "idx_udcs_detail_category_id", columnList = "detail_category_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserDetailCategorySubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detail_category_id", nullable = false)
    private DetailCategory detailCategory;

    @Column(nullable = false)
    @Builder.Default
    private Boolean subscribed = false;  // 구독 여부 (True/False 토글)

    /**
     * 구독 상태 변경
     */
    public void updateSubscribed(Boolean subscribed) {
        this.subscribed = subscribed;
    }

    /**
     * 구독 토글
     */
    public void toggleSubscription() {
        this.subscribed = !this.subscribed;
    }
}
