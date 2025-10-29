package com.incheon.notice.entity;

/**
 * 알림 전송 상태
 */
public enum NotificationStatus {
    SUCCESS,  // 전송 성공
    FAILED,   // 전송 실패
    PENDING   // 대기 중
}
