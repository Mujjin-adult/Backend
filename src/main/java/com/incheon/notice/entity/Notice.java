package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 공지사항 엔티티
 * 인천대학교 각 부서/학과에서 발행하는 공지사항 정보
 */
@Entity
@Table(name = "notices", indexes = {
    @Index(name = "idx_category_id", columnList = "category_id"),
    @Index(name = "idx_published_at", columnList = "published_at"),
    @Index(name = "idx_external_id", columnList = "external_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;  // 공지사항 제목

    @Column(columnDefinition = "TEXT")
    private String content;  // 공지사항 내용

    @Column(nullable = false, length = 1000)
    private String url;  // 원본 게시글 URL

    @Column(unique = true, length = 100)
    private String externalId;  // 외부 시스템의 게시글 ID (중복 방지용)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;  // 카테고리

    @Column(length = 100)
    private String author;  // 작성자

    @Column(nullable = false)
    private LocalDateTime publishedAt;  // 게시일

    @Column
    private Integer viewCount;  // 조회수 (크롤링한 값)

    @Column(nullable = false)
    @Builder.Default
    private Boolean isImportant = false;  // 중요 공지 여부

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPinned = false;  // 상단 고정 여부

    // 첨부파일 정보 (JSON 형태로 저장 가능)
    @Column(columnDefinition = "TEXT")
    private String attachments;

    // 이 공지사항을 북마크한 사용자 목록
    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Bookmark> bookmarks = new ArrayList<>();

    /**
     * 조회수 업데이트
     */
    public void updateViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    /**
     * 공지사항 정보 업데이트 (크롤링 시 변경사항 반영)
     */
    public void updateInfo(String title, String content, String author, Integer viewCount) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.viewCount = viewCount;
    }

    /**
     * 중요 공지 설정
     */
    public void markAsImportant(Boolean isImportant) {
        this.isImportant = isImportant;
    }

    /**
     * 상단 고정 설정
     */
    public void pin(Boolean isPinned) {
        this.isPinned = isPinned;
    }
}
