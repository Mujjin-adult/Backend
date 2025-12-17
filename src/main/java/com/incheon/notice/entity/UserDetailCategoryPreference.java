package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 상세 카테고리 구독 엔티티
 * 사용자가 어떤 상세 카테고리의 알림을 받을지 설정
 */
@Entity
@Table(name = "user_detail_category_preferences",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_detail_category", columnNames = {"user_id", "detail_category_id"})
    },
    indexes = {
        @Index(name = "idx_user_detail_pref_user_id", columnList = "user_id"),
        @Index(name = "idx_user_detail_pref_category_id", columnList = "detail_category_id"),
        @Index(name = "idx_user_detail_pref_enabled", columnList = "enabled")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserDetailCategoryPreference extends BaseEntity {

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
    private Boolean enabled = false;  // 구독 활성화 여부

    /**
     * 구독 상태 변경
     */
    public void updateEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
