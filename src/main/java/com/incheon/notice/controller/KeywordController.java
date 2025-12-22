package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.KeywordDto;
import com.incheon.notice.security.CustomUserDetailsService;
import com.incheon.notice.service.KeywordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 알림 키워드 API Controller
 * 사용자의 알림 키워드 등록, 조회, 삭제, 토글 기능
 */
@Tag(name = "알림 키워드", description = "키워드 기반 푸시 알림 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordService keywordService;

    /**
     * 내 키워드 목록 조회
     * GET /api/keywords
     */
    @Operation(
            summary = "내 키워드 목록 조회",
            description = "등록한 알림 키워드 목록을 조회합니다. 최신 등록순으로 정렬됩니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<KeywordDto.Response>>> getMyKeywords() {
        Long userId = getCurrentUserId();
        List<KeywordDto.Response> keywords = keywordService.getMyKeywords(userId);

        return ResponseEntity.ok(ApiResponse.success("키워드 목록 조회 성공", keywords));
    }

    /**
     * 키워드 등록
     * POST /api/keywords
     *
     * 키워드 등록 시 알림은 자동으로 활성화됩니다 (isActive = true)
     */
    @Operation(
            summary = "키워드 등록",
            description = """
                    알림 키워드를 등록합니다.

                    **기능:**
                    - 등록된 키워드가 포함된 새 공지사항이 올라오면 푸시 알림을 받습니다.
                    - 키워드 등록 시 알림은 **자동으로 활성화**됩니다.
                    - 최대 10개까지 등록 가능합니다.
                    - # 기호는 자동으로 제거됩니다.

                    **예시:**
                    - "장학금" → 장학금 관련 공지 알림
                    - "취업" → 취업 관련 공지 알림
                    - "등록금" → 등록금 관련 공지 알림
                    """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<KeywordDto.Response>> createKeyword(
            @Valid @RequestBody KeywordDto.CreateRequest request) {

        Long userId = getCurrentUserId();
        KeywordDto.Response keyword = keywordService.createKeyword(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("키워드가 등록되었습니다", keyword));
    }

    /**
     * 키워드 삭제
     * DELETE /api/keywords/{id}
     */
    @Operation(summary = "키워드 삭제", description = "등록한 알림 키워드를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteKeyword(
            @Parameter(description = "키워드 ID") @PathVariable Long id) {

        Long userId = getCurrentUserId();
        keywordService.deleteKeyword(userId, id);

        return ResponseEntity.ok(ApiResponse.success("키워드가 삭제되었습니다", null));
    }

    /**
     * 키워드 활성화/비활성화 스위치
     * PATCH /api/keywords/{id}/toggle
     *
     * 키워드를 삭제하지 않고 알림만 일시적으로 끄거나 켤 수 있습니다.
     */
    @Operation(
            summary = "키워드 알림 스위치",
            description = """
                    키워드의 알림 활성화 상태를 스위치합니다.

                    - 활성화 (isActive=true): 키워드 매칭 시 알림 발송
                    - 비활성화 (isActive=false): 키워드가 매칭되어도 알림 발송 안 함

                    키워드를 삭제하지 않고 일시적으로 알림을 끌 때 유용합니다.
                    """
    )
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<KeywordDto.Response>> toggleKeyword(
            @Parameter(description = "키워드 ID") @PathVariable Long id) {

        Long userId = getCurrentUserId();
        KeywordDto.Response keyword = keywordService.toggleKeyword(userId, id);

        String message = keyword.getIsActive()
                ? "키워드 알림이 활성화되었습니다"
                : "키워드 알림이 비활성화되었습니다";

        return ResponseEntity.ok(ApiResponse.success(message, keyword));
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
