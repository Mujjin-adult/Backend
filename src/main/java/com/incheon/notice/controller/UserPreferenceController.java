package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.UserPreferenceDto;
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
 * 상세 카테고리 구독 및 알림 관리
 */
@Tag(name = "알림 설정", description = "상세 카테고리 구독 및 알림 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    /**
     * 상세 카테고리 목록 조회 (구독 상태 포함)
     * GET /api/preferences/detail-categories
     */
    @Operation(
            summary = "상세 카테고리 목록 조회",
            description = "전체 상세 카테고리 목록을 사용자의 구독 상태와 함께 조회합니다."
    )
    @GetMapping("/detail-categories")
    public ResponseEntity<ApiResponse<List<UserPreferenceDto.DetailCategoryResponse>>> getDetailCategories() {
        Long userId = getCurrentUserId();
        List<UserPreferenceDto.DetailCategoryResponse> categories =
                userPreferenceService.getDetailCategoriesWithSubscriptionStatus(userId);

        return ResponseEntity.ok(ApiResponse.success("상세 카테고리 목록 조회 성공", categories));
    }

    /**
     * 알림 상세 카테고리 구독 설정
     * PATCH /api/preferences/categories
     */
    @Operation(
            summary = "알림 상세 카테고리 구독 설정",
            description = "상세 카테고리의 구독 상태를 일괄 업데이트합니다. 구독 활성화/비활성화를 설정할 수 있습니다."
    )
    @PatchMapping("/categories")
    public ResponseEntity<ApiResponse<List<UserPreferenceDto.DetailCategoryResponse>>> updateDetailCategorySubscriptions(
            @Valid @RequestBody UserPreferenceDto.DetailCategorySubscribeRequest request) {

        Long userId = getCurrentUserId();
        List<UserPreferenceDto.DetailCategoryResponse> categories =
                userPreferenceService.updateDetailCategorySubscriptions(userId, request);

        return ResponseEntity.ok(ApiResponse.success("상세 카테고리 구독 설정이 업데이트되었습니다", categories));
    }

    /**
     * 알림 활성화된 구독 카테고리만 조회
     * GET /api/preferences/categories/active
     */
    @Operation(summary = "활성 구독 조회", description = "알림이 활성화된 구독 카테고리만 조회합니다.")
    @GetMapping("/categories/active")
    public ResponseEntity<ApiResponse<List<UserPreferenceDto.Response>>> getActivePreferences() {
        Long userId = getCurrentUserId();
        List<UserPreferenceDto.Response> preferences = userPreferenceService.getActivePreferences(userId);

        return ResponseEntity.ok(ApiResponse.success("활성화된 구독 카테고리 조회 성공", preferences));
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
