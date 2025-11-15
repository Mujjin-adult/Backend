package com.incheon.notice.repository;

import com.incheon.notice.entity.Category;
import com.incheon.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 공지사항 Repository
 */
@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 외부 ID로 조회 (중복 체크용)
     */
    Optional<Notice> findByExternalId(String externalId);

    /**
     * 카테고리별 공지사항 페이징 조회 (최신순)
     */
    Page<Notice> findByCategoryOrderByPublishedAtDesc(Category category, Pageable pageable);

    /**
     * 카테고리별 공지사항 페이징 조회 (카테고리 ID로)
     */
    Page<Notice> findByCategoryIdOrderByPublishedAtDesc(Long categoryId, Pageable pageable);

    /**
     * 전체 공지사항 페이징 조회 (최신순)
     */
    Page<Notice> findAllByOrderByPublishedAtDesc(Pageable pageable);

    /**
     * 제목이나 내용에 키워드가 포함된 공지사항 검색
     */
    @Query("SELECT n FROM Notice n WHERE n.title LIKE CONCAT('%', :keyword, '%') OR n.content LIKE CONCAT('%', :keyword, '%') ORDER BY n.publishedAt DESC")
    Page<Notice> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 중요 공지사항 조회
     */
    List<Notice> findByIsImportantTrueOrderByPublishedAtDesc();

    /**
     * 상단 고정 공지사항 조회
     */
    List<Notice> findByIsPinnedTrueOrderByPublishedAtDesc();

    /**
     * 특정 기간 동안 게시된 공지사항 조회
     */
    @Query("SELECT n FROM Notice n WHERE n.publishedAt BETWEEN :startDate AND :endDate ORDER BY n.publishedAt DESC")
    List<Notice> findByPublishedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 카테고리별 최신 공지사항 N개 조회
     */
    List<Notice> findTop10ByCategoryOrderByPublishedAtDesc(Category category);

    /**
     * 외부 ID 존재 여부 확인
     */
    boolean existsByExternalId(String externalId);

    /**
     * 카테고리 코드별 공지사항 개수 조회
     */
    @Query("SELECT COUNT(n) FROM Notice n WHERE n.category.code = :categoryCode")
    long countByCategoryCode(@Param("categoryCode") String categoryCode);

    /**
     * 특정 시간 이후 생성된 공지사항 개수 조회
     */
    long countByCreatedAtAfter(LocalDateTime createdAt);
}
