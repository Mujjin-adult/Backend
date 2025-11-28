package com.incheon.notice.service;

import com.incheon.notice.entity.CrawlNotice;
import com.incheon.notice.entity.NotificationKeyword;
import com.incheon.notice.entity.User;
import com.incheon.notice.repository.NotificationKeywordRepository;
import com.incheon.notice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 알림 서비스
 * 키워드 기반 푸시 알림 발송 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationKeywordRepository notificationKeywordRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    /**
     * 새 공지사항에 대해 키워드 매칭 및 알림 발송
     *
     * 웹훅에서 호출되어 크롤러가 새로 등록한 공지사항을 처리합니다.
     *
     * @param crawlNotice 새로 등록된 공지사항
     * @return 발송된 알림 개수
     */
    @Transactional
    public int processNewNotice(CrawlNotice crawlNotice) {
        if (crawlNotice == null) {
            log.warn("CrawlNotice is null, skipping notification");
            return 0;
        }

        String title = crawlNotice.getTitle();
        String content = crawlNotice.getContent() != null ? crawlNotice.getContent() : "";
        Long categoryId = crawlNotice.getCategoryId();

        log.info("Processing new notice for notifications: id={}, title={}, categoryId={}",
                crawlNotice.getId(), title, categoryId);

        // 1. 매칭되는 키워드 조회
        List<NotificationKeyword> matchingKeywords = notificationKeywordRepository
                .findMatchingKeywords(title, content, categoryId);

        if (matchingKeywords.isEmpty()) {
            log.debug("No matching keywords found for notice: id={}", crawlNotice.getId());
            return 0;
        }

        log.info("Found {} matching keywords for notice: id={}", matchingKeywords.size(), crawlNotice.getId());

        // 2. 사용자별로 그룹화 (같은 사용자가 여러 키워드를 등록한 경우 한 번만 알림)
        Map<Long, List<NotificationKeyword>> keywordsByUser = matchingKeywords.stream()
                .collect(Collectors.groupingBy(nk -> nk.getUser().getId()));

        // 3. FCM 토큰 수집 및 알림 발송
        List<String> fcmTokens = matchingKeywords.stream()
                .map(nk -> nk.getUser().getFcmToken())
                .distinct()
                .filter(token -> token != null && !token.isEmpty())
                .collect(Collectors.toList());

        if (fcmTokens.isEmpty()) {
            log.warn("No valid FCM tokens found for matching keywords");
            return 0;
        }

        // 4. 알림 데이터 구성
        String notificationTitle = "새 공지사항이 등록되었습니다";
        String notificationBody = truncate(title, 100);

        Map<String, String> data = new HashMap<>();
        data.put("type", "new_notice");
        data.put("noticeId", String.valueOf(crawlNotice.getId()));
        data.put("noticeTitle", title);
        data.put("noticeUrl", crawlNotice.getUrl());
        if (categoryId != null) {
            data.put("categoryId", String.valueOf(categoryId));
        }

        // 5. FCM 일괄 발송
        int successCount = fcmService.sendBatchNotification(
                fcmTokens,
                notificationTitle,
                notificationBody,
                data
        );

        // 6. 알림 통계 업데이트 (매칭 카운트, 마지막 알림 시각)
        updateKeywordStatistics(matchingKeywords);

        log.info("Notification processing completed: noticeId={}, matched={}, sent={}, success={}",
                crawlNotice.getId(), matchingKeywords.size(), fcmTokens.size(), successCount);

        return successCount;
    }

    /**
     * 키워드 통계 업데이트 (매칭 카운트, 마지막 알림 시각)
     */
    private void updateKeywordStatistics(List<NotificationKeyword> keywords) {
        for (NotificationKeyword keyword : keywords) {
            keyword.incrementMatchedCount();
            keyword.updateLastNotifiedAt();
        }
        notificationKeywordRepository.saveAll(keywords);
    }

    /**
     * 특정 사용자에게 테스트 알림 발송
     *
     * @param userId 사용자 ID
     * @return 성공 여부
     */
    @Transactional(readOnly = true)
    public boolean sendTestNotification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        String fcmToken = user.getFcmToken();
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("User has no FCM token: userId={}", userId);
            return false;
        }

        String title = "띠링인캠퍼스 테스트 알림";
        String body = "알림이 정상적으로 작동하고 있습니다 ✅";

        Map<String, String> data = new HashMap<>();
        data.put("type", "test");
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        boolean success = fcmService.sendNotification(fcmToken, title, body, data);

        if (success) {
            log.info("Test notification sent successfully: userId={}, email={}", userId, user.getEmail());
        } else {
            log.error("Failed to send test notification: userId={}", userId);
        }

        return success;
    }

    /**
     * 중요 공지사항 전체 알림 발송 (토픽 사용)
     *
     * @param crawlNotice 중요 공지사항
     * @return 성공 여부
     */
    @Transactional(readOnly = true)
    public boolean sendImportantNoticeToAll(CrawlNotice crawlNotice) {
        if (crawlNotice == null || !Boolean.TRUE.equals(crawlNotice.getIsImportant())) {
            log.warn("Notice is not important, skipping broadcast");
            return false;
        }

        String title = "[중요] 새 공지사항";
        String body = truncate(crawlNotice.getTitle(), 100);

        Map<String, String> data = new HashMap<>();
        data.put("type", "important_notice");
        data.put("noticeId", String.valueOf(crawlNotice.getId()));
        data.put("noticeTitle", crawlNotice.getTitle());
        data.put("noticeUrl", crawlNotice.getUrl());

        // 'all_users' 토픽으로 전체 발송
        boolean success = fcmService.sendTopicNotification("all_users", title, body, data);

        if (success) {
            log.info("Important notice broadcasted to all users: noticeId={}, title={}",
                    crawlNotice.getId(), crawlNotice.getTitle());
        } else {
            log.error("Failed to broadcast important notice: noticeId={}", crawlNotice.getId());
        }

        return success;
    }

    /**
     * 카테고리별 알림 발송
     *
     * @param crawlNotice 공지사항
     * @param categoryName 카테고리 이름
     * @return 성공 여부
     */
    @Transactional(readOnly = true)
    public boolean sendCategoryNotification(CrawlNotice crawlNotice, String categoryName) {
        if (crawlNotice == null || crawlNotice.getCategoryId() == null) {
            log.warn("Notice has no category, skipping category notification");
            return false;
        }

        String title = String.format("[%s] 새 공지사항", categoryName);
        String body = truncate(crawlNotice.getTitle(), 100);

        Map<String, String> data = new HashMap<>();
        data.put("type", "category_notice");
        data.put("noticeId", String.valueOf(crawlNotice.getId()));
        data.put("categoryId", String.valueOf(crawlNotice.getCategoryId()));
        data.put("categoryName", categoryName);
        data.put("noticeTitle", crawlNotice.getTitle());
        data.put("noticeUrl", crawlNotice.getUrl());

        // 카테고리별 토픽으로 발송 (예: "category_1")
        String topic = "category_" + crawlNotice.getCategoryId();
        boolean success = fcmService.sendTopicNotification(topic, title, body, data);

        if (success) {
            log.info("Category notice sent to topic: topic={}, noticeId={}", topic, crawlNotice.getId());
        } else {
            log.error("Failed to send category notice: topic={}, noticeId={}", topic, crawlNotice.getId());
        }

        return success;
    }

    /**
     * 문자열 truncate (최대 길이로 자르기)
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
