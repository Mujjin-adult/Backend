package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.UserPreferenceDto;
import com.incheon.notice.security.CustomUserDetailsService;
import com.incheon.notice.service.UserPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 알림 설정 API Controller
 * 카테고리 구독 및 알림 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    /**
     * 카테고리 구독
     * POST /api/preferences/categories
     */
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<UserPreferenceDto.Response>> subscribeCategory(
            @Valid @RequestBody UserPreferenceDto.SubscribeRequest request) {

        Long userId = getCurrentUserId();
        UserPreferenceDto.Response preference = userPreferenceService.subscribe(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("카테고리 구독이 완료되었습니다", preference));
    }

    /**
     * 내 구독 카테고리 목록 조회
     * GET /api/preferences/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<UserPreferenceDto.Response>>> getMyPreferences() {
        Long userId = getCurrentUserId();
        List<UserPreferenceDto.Response> preferences = userPreferenceService.getMyPreferences(userId);

        return ResponseEntity.ok(ApiResponse.success("구독 카테고리 목록 조회 성공", preferences));
    }

    /**
     * 알림 활성화된 구독 카테고리만 조회
     * GET /api/preferences/categories/active
     */
    @GetMapping("/categories/active")
    public ResponseEntity<ApiResponse<List<UserPreferenceDto.Response>>> getActivePreferences() {
        Long userId = getCurrentUserId();
        List<UserPreferenceDto.Response> preferences = userPreferenceService.getActivePreferences(userId);

        return ResponseEntity.ok(ApiResponse.success("활성화된 구독 카테고리 조회 성공", preferences));
    }

    /**
     * 특정 카테고리 구독 여부 확인
     * GET /api/preferences/categories/{categoryId}/subscribed
     */
    @GetMapping("/categories/{categoryId}/subscribed")
    public ResponseEntity<ApiResponse<Boolean>> isSubscribed(@PathVariable Long categoryId) {
        Long userId = getCurrentUserId();
        boolean isSubscribed = userPreferenceService.isSubscribed(userId, categoryId);

        return ResponseEntity.ok(ApiResponse.success(isSubscribed));
    }

    /**
     * 알림 설정 변경 (활성화/비활성화)
     * PUT /api/preferences/categories/{categoryId}/notification
     */
    @PutMapping("/categories/{categoryId}/notification")
    public ResponseEntity<ApiResponse<UserPreferenceDto.Response>> updateNotification(
            @PathVariable Long categoryId,
            @Valid @RequestBody UserPreferenceDto.UpdateNotificationRequest request) {

        Long userId = getCurrentUserId();
        UserPreferenceDto.Response preference = userPreferenceService.updateNotification(userId, categoryId, request);

        return ResponseEntity.ok(ApiResponse.success("알림 설정이 변경되었습니다", preference));
    }

    /**
     * 카테고리 구독 취소
     * DELETE /api/preferences/categories/{categoryId}
     */
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> unsubscribeCategory(@PathVariable Long categoryId) {
        Long userId = getCurrentUserId();
        userPreferenceService.unsubscribe(userId, categoryId);

        return ResponseEntity.ok(ApiResponse.success("카테고리 구독이 취소되었습니다", null));
    }

    /**
     * SecurityContext에서 현재 인증된 사용자 ID 가져오기
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증이 필요합니다");
        }

        return ((CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal()).getUserId();
    }
}
