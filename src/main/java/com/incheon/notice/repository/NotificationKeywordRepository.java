package com.incheon.notice.repository;

import com.incheon.notice.entity.NotificationKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 알림 키워드 Repository
 */
@Repository
public interface NotificationKeywordRepository extends JpaRepository<NotificationKeyword, Long> {

    /**
     * 사용자의 알림 키워드 목록 조회
     */
    List<NotificationKeyword> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자의 활성화된 알림 키워드 목록 조회
     */
    List<NotificationKeyword> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자와 키워드로 조회
     */
    Optional<NotificationKeyword> findByUserIdAndKeyword(Long userId, String keyword);

    /**
     * 사용자의 특정 키워드 존재 여부 확인
     */
    boolean existsByUserIdAndKeyword(Long userId, String keyword);

    /**
     * 사용자의 알림 키워드 개수
     */
    long countByUserId(Long userId);

    /**
     * 사용자의 활성화된 알림 키워드 개수
     */
    long countByUserIdAndIsActiveTrue(Long userId);

    /**
     * 특정 키워드를 등록한 모든 활성 사용자 조회
     * (FCM 토큰이 있는 사용자만)
     */
    @Query("SELECT nk FROM NotificationKeyword nk " +
            "JOIN FETCH nk.user u " +
            "WHERE nk.keyword = :keyword " +
            "AND nk.isActive = true " +
            "AND u.fcmToken IS NOT NULL " +
            "AND u.isActive = true")
    List<NotificationKeyword> findActiveUsersByKeyword(@Param("keyword") String keyword);

    /**
     * 새 공지사항과 매칭되는 모든 활성 키워드 조회
     * (FCM 토큰이 있는 사용자만)
     *
     * @param title 공지사항 제목
     * @param categoryId 공지사항 카테고리 ID (null 가능)
     * @return 매칭되는 알림 키워드 목록
     */
    @Query("SELECT nk FROM NotificationKeyword nk " +
            "JOIN FETCH nk.user u " +
            "WHERE nk.isActive = true " +
            "AND u.fcmToken IS NOT NULL " +
            "AND u.isActive = true " +
            "AND (" +
            "  :title LIKE CONCAT('%', nk.keyword, '%') " +
            "  OR :content LIKE CONCAT('%', nk.keyword, '%')" +
            ") " +
            "AND (nk.categoryId IS NULL OR nk.categoryId = :categoryId)")
    List<NotificationKeyword> findMatchingKeywords(
            @Param("title") String title,
            @Param("content") String content,
            @Param("categoryId") Long categoryId
    );

    /**
     * 카테고리별 활성 키워드 조회
     */
    List<NotificationKeyword> findByCategoryIdAndIsActiveTrueOrderByCreatedAtDesc(Long categoryId);

    /**
     * 특정 사용자의 카테고리별 활성 키워드 조회
     */
    List<NotificationKeyword> findByUserIdAndCategoryIdAndIsActiveTrueOrderByCreatedAtDesc(
            Long userId,
            Long categoryId
    );
}
