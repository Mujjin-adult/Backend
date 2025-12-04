package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.DetailCategoryDto;
import com.incheon.notice.security.CustomUserDetailsService;
import com.incheon.notice.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 알림 설정 API Controller
 * detail_category 기반 카테고리 구독 관리
 */
@Tag(name = "알림 설정", description = "상세 카테고리 구독 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    /**
     * 전체 상세 카테고리 목록 조회 (사용자의 구독 여부 포함)
     * GET /api/preferences/detail-categories
     */
    @Operation(summary = "상세 카테고리 목록 조회", description = "전체 상세 카테고리 목록과 현재 사용자의 구독 여부를 조회합니다.")
    @GetMapping("/detail-categories")
    public ResponseEntity<ApiResponse<DetailCategoryDto.ListResponse>> getDetailCategories() {
        Long userId = getCurrentUserId();
        DetailCategoryDto.ListResponse response = userPreferenceService.getDetailCategoriesWithSubscription(userId);

        return ResponseEntity.ok(ApiResponse.success("상세 카테고리 목록 조회 성공", response));
    }

    /**
     * 활성화된 구독 카테고리만 조회
     * GET /api/preferences/categories/active
     */
    @Operation(summary = "활성 구독 조회", description = "현재 사용자가 구독 중인 상세 카테고리만 조회합니다.")
    @GetMapping("/categories/active")
    public ResponseEntity<ApiResponse<List<DetailCategoryDto.Response>>> getActiveSubscriptions() {
        Long userId = getCurrentUserId();
        List<DetailCategoryDto.Response> subscriptions = userPreferenceService.getActiveSubscriptions(userId);

        return ResponseEntity.ok(ApiResponse.success("활성 구독 카테고리 조회 성공", subscriptions));
    }

    /**
     * 상세 카테고리 구독 설정 일괄 변경
     * PATCH /api/preferences/detail-categories
     */
    @Operation(summary = "구독 일괄 변경", description = "여러 상세 카테고리의 구독 상태를 일괄 변경합니다.")
    @PatchMapping("/detail-categories")
    public ResponseEntity<ApiResponse<DetailCategoryDto.ListResponse>> updateSubscriptions(
            @Valid @RequestBody DetailCategoryDto.BatchSubscriptionRequest request) {

        Long userId = getCurrentUserId();
        DetailCategoryDto.ListResponse response = userPreferenceService.updateDetailCategorySubscriptions(userId, request);

        return ResponseEntity.ok(ApiResponse.success("구독 설정이 변경되었습니다", response));
    }

    /**
     * 단일 카테고리 구독 토글
     * POST /api/preferences/detail-categories/{categoryId}/toggle
     */
    @Operation(summary = "구독 토글", description = "특정 상세 카테고리의 구독 상태를 토글합니다.")
    @PostMapping("/detail-categories/{categoryId}/toggle")
    public ResponseEntity<ApiResponse<DetailCategoryDto.Response>> toggleSubscription(
            @PathVariable Long categoryId) {

        Long userId = getCurrentUserId();
        DetailCategoryDto.Response response = userPreferenceService.toggleSubscription(userId, categoryId);

        return ResponseEntity.ok(ApiResponse.success("구독 상태가 변경되었습니다", response));
    }

    /**
     * 특정 카테고리 구독 여부 확인
     * GET /api/preferences/detail-categories/{categoryId}/subscribed
     */
    @Operation(summary = "구독 여부 확인", description = "특정 상세 카테고리를 구독하고 있는지 확인합니다.")
    @GetMapping("/detail-categories/{categoryId}/subscribed")
    public ResponseEntity<ApiResponse<Boolean>> isSubscribed(@PathVariable Long categoryId) {
        Long userId = getCurrentUserId();
        boolean isSubscribed = userPreferenceService.isSubscribed(userId, categoryId);

        return ResponseEntity.ok(ApiResponse.success(isSubscribed));
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
