package com.incheon.notice.repository;

import com.incheon.notice.entity.CrawlNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 크롤링 공지사항 Repository
 */
@Repository
public interface CrawlNoticeRepository extends JpaRepository<CrawlNotice, Long>, JpaSpecificationExecutor<CrawlNotice> {

    /**
     * 외부 ID로 조회 (중복 체크용)
     */
    Optional<CrawlNotice> findByExternalId(String externalId);

    /**
     * Fingerprint로 조회 (중복 체크용)
     */
    Optional<CrawlNotice> findByFingerprint(String fingerprint);

    /**
     * URL로 조회
     */
    Optional<CrawlNotice> findByUrl(String url);

    /**
     * 카테고리 ID별 공지사항 페이징 조회 (최신순)
     */
    Page<CrawlNotice> findByCategoryIdOrderByPublishedAtDesc(Long categoryId, Pageable pageable);

    /**
     * 전체 공지사항 페이징 조회 (최신순)
     */
    Page<CrawlNotice> findAllByOrderByPublishedAtDesc(Pageable pageable);

    /**
     * 전체 공지사항 페이징 조회 (생성일 최신순)
     */
    Page<CrawlNotice> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 제목이나 내용에 키워드가 포함된 공지사항 검색
     */
    @Query("SELECT cn FROM CrawlNotice cn WHERE cn.title LIKE CONCAT('%', :keyword, '%') OR cn.content LIKE CONCAT('%', :keyword, '%') ORDER BY cn.publishedAt DESC")
    Page<CrawlNotice> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 중요 공지사항 조회
     */
    List<CrawlNotice> findByIsImportantTrueOrderByPublishedAtDesc();

    /**
     * 상단 고정 공지사항 조회
     */
    List<CrawlNotice> findByIsPinnedTrueOrderByPublishedAtDesc();

    /**
     * 특정 기간 동안 게시된 공지사항 조회
     */
    @Query("SELECT cn FROM CrawlNotice cn WHERE cn.publishedAt BETWEEN :startDate AND :endDate ORDER BY cn.publishedAt DESC")
    List<CrawlNotice> findByPublishedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 카테고리 ID별 최신 공지사항 N개 조회
     */
    List<CrawlNotice> findTop10ByCategoryIdOrderByPublishedAtDesc(Long categoryId);

    /**
     * 외부 ID 존재 여부 확인
     */
    boolean existsByExternalId(String externalId);

    /**
     * Fingerprint 존재 여부 확인
     */
    boolean existsByFingerprint(String fingerprint);

    /**
     * URL 존재 여부 확인
     */
    boolean existsByUrl(String url);

    /**
     * 카테고리 ID별 공지사항 개수 조회
     */
    long countByCategoryId(Long categoryId);

    /**
     * 특정 시간 이후 생성된 공지사항 개수 조회
     */
    long countByCreatedAtAfter(LocalDateTime createdAt);

    /**
     * 소스별 공지사항 조회 (크롤러 기능)
     */
    Page<CrawlNotice> findBySourceOrderByCreatedAtDesc(String source, Pageable pageable);

    /**
     * Job ID별 공지사항 조회 (크롤러 기능)
     */
    Page<CrawlNotice> findByJobIdOrderByCreatedAtDesc(Long jobId, Pageable pageable);

    /**
     * Job ID별 공지사항 개수 조회
     */
    long countByJobId(Long jobId);

    /**
     * 소스별 공지사항 개수 조회
     */
    long countBySource(String source);

    /**
     * 특정 카테고리 이름 목록에 해당하는 공지사항 조회 (구독 공지사항용)
     */
    @Query("SELECT cn FROM CrawlNotice cn WHERE cn.category IN :categoryNames ORDER BY cn.publishedAt DESC")
    Page<CrawlNotice> findByCategoryIn(@Param("categoryNames") List<String> categoryNames, Pageable pageable);

    /**
     * 특정 카테고리 ID 목록에 해당하는 공지사항 조회 (구독 공지사항용)
     */
    @Query("SELECT cn FROM CrawlNotice cn WHERE cn.categoryId IN :categoryIds ORDER BY cn.createdAt DESC")
    Page<CrawlNotice> findByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds, Pageable pageable);
}
