package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 크롤링 공지사항 엔티티 (crawl_notice 테이블)
 * 크롤러에서 수집한 공지사항 정보 + 사용자 기능 통합
 */
@Entity
@Table(name = "crawl_notice", indexes = {
    @Index(name = "idx_job_id", columnList = "job_id"),
    @Index(name = "idx_url", columnList = "url"),
    @Index(name = "idx_category_id", columnList = "category_id"),
    @Index(name = "idx_published_at", columnList = "published_at"),
    @Index(name = "idx_external_id", columnList = "external_id"),
    @Index(name = "idx_fingerprint", columnList = "fingerprint"),
    @Index(name = "idx_is_important", columnList = "is_important"),
    @Index(name = "idx_is_pinned", columnList = "is_pinned")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrawlNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== 크롤러 메타데이터 ====================

    @Column(name = "job_id", nullable = false)
    private Long jobId;  // FK to crawl_job.id

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;  // 원본 게시글 URL

    @Column(length = 64, nullable = false)
    private String fingerprint;  // 콘텐츠 지문 (중복 체크)

    @Column(length = 32)
    private String snapshotVersion;  // 스냅샷 버전

    @Column(length = 64)
    private String source;  // 크롤링 소스 (volunteer, job, scholarship 등)

    @Column(columnDefinition = "TEXT")
    private String raw;  // 원본 HTML (필요시)

    // ==================== 공지사항 기본 정보 ====================

    @Column(columnDefinition = "TEXT")
    private String title;  // 공지사항 제목

    @Column(columnDefinition = "TEXT")
    private String content;  // 공지사항 내용

    @Column(unique = true, length = 100)
    private String externalId;  // 외부 시스템의 게시글 ID (중복 방지용)

    @Column(length = 128)
    private String writer;  // 작성자 (크롤러에서 추출)

    @Column(length = 100)
    private String author;  // 작성자 (메인 서버 호환용 - writer와 동일)

    @Column(length = 32)
    private String date;  // 날짜 문자열 (크롤러 원본)

    @Column
    private LocalDateTime publishedAt;  // 게시일 (파싱된 날짜)

    @Column(length = 32)
    private String hits;  // 조회수 문자열 (크롤러 원본)

    @Column
    private Integer viewCount;  // 조회수 (파싱된 숫자)

    // ==================== 카테고리 및 분류 ====================

    @Column(length = 64)
    private String category;  // 카테고리 문자열 (크롤러)

    @Column(name = "category_id")
    private Long categoryId;  // FK to categories.id (메인 서버와 연동)

    // ==================== 사용자 기능 필드 ====================

    @Column(nullable = false)
    @Builder.Default
    private Boolean isImportant = false;  // 중요 공지 여부

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPinned = false;  // 상단 고정 여부

    @Column(columnDefinition = "TEXT")
    private String attachments;  // 첨부파일 정보 (JSON 형태)

    // ==================== Relationships ====================

    // 이 공지사항을 북마크한 사용자 목록
    @OneToMany(mappedBy = "crawlNotice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Bookmark> bookmarks = new ArrayList<>();

    // ==================== Business Methods ====================

    /**
     * 조회수 업데이트
     */
    public void updateViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    /**
     * 조회수 증가 (1씩 증가)
     */
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    /**
     * 공지사항 정보 업데이트 (크롤링 시 변경사항 반영)
     */
    public void updateInfo(String title, String content, String author, Integer viewCount) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.writer = author;  // writer도 함께 업데이트
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

    /**
     * 카테고리 ID 설정
     */
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * 게시일 설정
     */
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
