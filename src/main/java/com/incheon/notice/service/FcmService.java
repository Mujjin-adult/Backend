package com.incheon.notice.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FCM(Firebase Cloud Messaging) 서비스
 * 푸시 알림 발송 기능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    /**
     * 단일 디바이스에 푸시 알림 발송
     *
     * @param fcmToken 디바이스 FCM 토큰
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터 (선택사항)
     * @return 성공 여부
     */
    public boolean sendNotification(String fcmToken, String title, String body, Map<String, String> data) {
        try {
            // Notification 빌더
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // Message 빌더
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification);

            // 추가 데이터가 있으면 설정
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            // Android 설정 (선택사항)
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setSound("default")
                            .setColor("#FF6B35")  // 알림 색상 (인천대 메인 컬러)
                            .build())
                    .build();
            messageBuilder.setAndroidConfig(androidConfig);

            // APNS (iOS) 설정 (선택사항)
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setSound("default")
                            .build())
                    .build();
            messageBuilder.setApnsConfig(apnsConfig);

            Message message = messageBuilder.build();

            // FCM 메시지 전송
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM notification sent successfully: token={}, title={}, response={}",
                    maskToken(fcmToken), title, response);

            return true;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification: token={}, title={}, error={}",
                    maskToken(fcmToken), title, e.getMessage(), e);

            // 토큰이 유효하지 않은 경우
            if (isInvalidTokenError(e)) {
                log.warn("Invalid FCM token detected: {}", maskToken(fcmToken));
                // TODO: 유효하지 않은 토큰 제거 로직 추가 (User 테이블에서 fcmToken null 처리)
            }

            return false;
        }
    }

    /**
     * 여러 디바이스에 푸시 알림 일괄 발송
     *
     * @param fcmTokens 디바이스 FCM 토큰 목록 (최대 500개)
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터 (선택사항)
     * @return 성공한 토큰 수
     */
    public int sendBatchNotification(List<String> fcmTokens, String title, String body, Map<String, String> data) {
        if (fcmTokens == null || fcmTokens.isEmpty()) {
            log.warn("No FCM tokens provided for batch notification");
            return 0;
        }

        // FCM은 한 번에 최대 500개의 메시지만 보낼 수 있음
        if (fcmTokens.size() > 500) {
            log.warn("FCM token count exceeds 500, splitting into batches");
            return sendBatchNotificationInChunks(fcmTokens, title, body, data);
        }

        try {
            // Notification 빌더
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // MulticastMessage 빌더
            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllTokens(fcmTokens)
                    .setNotification(notification);

            // 추가 데이터가 있으면 설정
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            // Android 설정
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setSound("default")
                            .setColor("#FF6B35")
                            .build())
                    .build();
            messageBuilder.setAndroidConfig(androidConfig);

            // APNS (iOS) 설정
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setSound("default")
                            .build())
                    .build();
            messageBuilder.setApnsConfig(apnsConfig);

            MulticastMessage message = messageBuilder.build();

            // FCM 일괄 전송
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            int successCount = response.getSuccessCount();
            int failureCount = response.getFailureCount();

            log.info("FCM batch notification sent: total={}, success={}, failure={}, title={}",
                    fcmTokens.size(), successCount, failureCount, title);

            // 실패한 토큰 로깅
            if (failureCount > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse sendResponse = responses.get(i);
                    if (!sendResponse.isSuccessful()) {
                        String token = fcmTokens.get(i);
                        Exception exception = sendResponse.getException();
                        log.warn("Failed to send to token {}: {}",
                                maskToken(token), exception != null ? exception.getMessage() : "Unknown error");

                        // 유효하지 않은 토큰 체크
                        if (exception instanceof FirebaseMessagingException) {
                            FirebaseMessagingException fme = (FirebaseMessagingException) exception;
                            if (isInvalidTokenError(fme)) {
                                log.warn("Invalid FCM token detected in batch: {}", maskToken(token));
                                // TODO: 유효하지 않은 토큰 제거 로직 추가
                            }
                        }
                    }
                }
            }

            return successCount;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM batch notification: title={}, tokenCount={}, error={}",
                    title, fcmTokens.size(), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 500개 이상의 토큰을 여러 배치로 나누어 전송
     */
    private int sendBatchNotificationInChunks(List<String> fcmTokens, String title, String body, Map<String, String> data) {
        int totalSuccess = 0;
        int chunkSize = 500;

        for (int i = 0; i < fcmTokens.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, fcmTokens.size());
            List<String> chunk = fcmTokens.subList(i, end);

            log.info("Sending batch {}/{}: tokens {}-{}",
                    (i / chunkSize) + 1, (fcmTokens.size() + chunkSize - 1) / chunkSize, i, end - 1);

            int successCount = sendBatchNotification(chunk, title, body, data);
            totalSuccess += successCount;

            // FCM 레이트 리밋 방지를 위한 지연 (선택사항)
            if (end < fcmTokens.size()) {
                try {
                    Thread.sleep(100); // 100ms 지연
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Batch notification interrupted");
                    break;
                }
            }
        }

        return totalSuccess;
    }

    /**
     * 토픽에 푸시 알림 발송
     *
     * @param topic 토픽 이름 (예: "all_users", "category_1")
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터 (선택사항)
     * @return 성공 여부
     */
    public boolean sendTopicNotification(String topic, String title, String body, Map<String, String> data) {
        try {
            // Notification 빌더
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // Message 빌더
            Message.Builder messageBuilder = Message.builder()
                    .setTopic(topic)
                    .setNotification(notification);

            // 추가 데이터가 있으면 설정
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            // Android 설정
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setSound("default")
                            .setColor("#FF6B35")
                            .build())
                    .build();
            messageBuilder.setAndroidConfig(androidConfig);

            // APNS (iOS) 설정
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setSound("default")
                            .build())
                    .build();
            messageBuilder.setApnsConfig(apnsConfig);

            Message message = messageBuilder.build();

            // FCM 토픽 메시지 전송
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM topic notification sent successfully: topic={}, title={}, response={}",
                    topic, title, response);

            return true;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM topic notification: topic={}, title={}, error={}",
                    topic, title, e.getMessage(), e);
            return false;
        }
    }

    /**
     * FCM 토큰이 유효하지 않은 에러인지 확인
     */
    private boolean isInvalidTokenError(FirebaseMessagingException e) {
        String errorCode = e.getMessagingErrorCode() != null
                ? e.getMessagingErrorCode().name()
                : "";

        return errorCode.equals("UNREGISTERED") ||
                errorCode.equals("INVALID_ARGUMENT") ||
                e.getMessage().contains("not a valid FCM registration token");
    }

    /**
     * 토큰 마스킹 (로그용)
     * 예: "dW4f2...k9L1m" (앞 5자, 뒤 5자만 표시)
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 10) {
            return "***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}
