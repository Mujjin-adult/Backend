package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 푸시 알림 이력 엔티티
 * 전송된 푸시 알림의 기록을 저장
 */
@Entity
@Table(name = "notification_history", indexes = {
    @Index(name = "idx_notif_user_id", columnList = "user_id"),
    @Index(name = "idx_notif_crawl_notice_id", columnList = "crawl_notice_id"),
    @Index(name = "idx_notif_sent_at", columnList = "sent_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 알림을 받은 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_notice_id", nullable = false)
    private CrawlNotice crawlNotice;  // 알림 대상 크롤링 공지사항

    @Column(nullable = false, length = 200)
    private String title;  // 알림 제목

    @Column(nullable = false, length = 500)
    private String body;  // 알림 내용

    @Column(nullable = false)
    private LocalDateTime sentAt;  // 전송 시각

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;  // 전송 상태

    @Column(length = 500)
    private String errorMessage;  // 실패 시 에러 메시지

    /**
     * 전송 상태 업데이트
     */
    public void updateStatus(NotificationStatus status, String errorMessage) {
        this.status = status;
        this.errorMessage = errorMessage;
    }
}

