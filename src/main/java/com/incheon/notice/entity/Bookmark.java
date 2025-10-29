package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 북마크 엔티티
 * 사용자가 저장한 공지사항 정보
 */
@Entity
@Table(name = "bookmarks",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_notice", columnNames = {"user_id", "notice_id"})
    },
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_notice_id", columnList = "notice_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Bookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;  // 공지사항

    @Column(length = 500)
    private String memo;  // 사용자 메모

    /**
     * 메모 수정
     */
    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
