package com.incheon.notice.repository;

import com.incheon.notice.entity.NotificationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 푸시 알림 이력 Repository
 */
@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    /**
     * 사용자의 알림 이력 조회 (페이징)
     */
    Page<NotificationHistory> findByUserIdOrderBySentAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자가 특정 공지사항에 대한 알림을 받았는지 확인
     */
    boolean existsByUserIdAndCrawlNoticeId(Long userId, Long crawlNoticeId);

    /**
     * 특정 기간 동안 전송된 알림 이력 조회
     */
    @Query("SELECT nh FROM NotificationHistory nh WHERE nh.sentAt BETWEEN :startDate AND :endDate ORDER BY nh.sentAt DESC")
    List<NotificationHistory> findBySentAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 실패한 알림 이력 조회
     */
    @Query("SELECT nh FROM NotificationHistory nh WHERE nh.status = 'FAILED' ORDER BY nh.sentAt DESC")
    List<NotificationHistory> findFailedNotifications();
}
