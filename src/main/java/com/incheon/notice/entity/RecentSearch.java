package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 최근 검색어 엔티티
 * 사용자별 최근 검색어 5개를 저장
 */
@Entity
@Table(name = "recent_searches",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_keyword", columnNames = {"user_id", "keyword"})
    },
    indexes = {
        @Index(name = "idx_recent_search_user_id", columnList = "user_id"),
        @Index(name = "idx_recent_search_searched_at", columnList = "searched_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecentSearch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String keyword;  // 검색 키워드

    /**
     * 검색어 업데이트 (검색 시각 갱신)
     */
    public void updateSearchedAt() {
        // createdAt이 자동 갱신됨 (BaseEntity의 @LastModifiedDate)
    }
}
