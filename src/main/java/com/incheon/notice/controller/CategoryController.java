package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.CategoryDto;
import com.incheon.notice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 카테고리 API Controller
 */
@Tag(name = "카테고리", description = "카테고리 목록 조회 API")
@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 전체 카테고리 목록 조회
     * GET /api/categories
     */
    @GetMapping
    @Operation(
            summary = "전체 카테고리 목록 조회",
            description = "모든 카테고리의 목록을 조회합니다. 각 카테고리의 공지사항 개수도 함께 반환됩니다."
    )
    public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getAllCategories() {
        log.info("전체 카테고리 목록 조회 API 호출");

        try {
            List<CategoryDto.Response> categories = categoryService.getAllCategories();

            return ResponseEntity.ok(
                    ApiResponse.success("카테고리 목록 조회 성공", categories)
            );
        } catch (Exception e) {
            log.error("카테고리 목록 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 활성 카테고리 목록 조회
     * GET /api/categories/active
     */
    @GetMapping("/active")
    @Operation(
            summary = "활성 카테고리 목록 조회",
            description = "활성 상태인 카테고리만 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getActiveCategories() {
        log.info("활성 카테고리 목록 조회 API 호출");

        try {
            List<CategoryDto.Response> categories = categoryService.getActiveCategories();

            return ResponseEntity.ok(
                    ApiResponse.success("활성 카테고리 조회 성공", categories)
            );
        } catch (Exception e) {
            log.error("활성 카테고리 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 특정 카테고리 조회 (코드로)
     * GET /api/categories/{code}
     */
    @GetMapping("/{code}")
    @Operation(
            summary = "특정 카테고리 조회",
            description = "카테고리 코드로 특정 카테고리 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<CategoryDto.Response>> getCategoryByCode(
            @PathVariable String code) {

        log.info("카테고리 조회 API 호출: code={}", code);

        try {
            CategoryDto.Response category = categoryService.getCategoryByCode(code);

            return ResponseEntity.ok(
                    ApiResponse.success("카테고리 조회 성공", category)
            );
        } catch (RuntimeException e) {
            log.error("카테고리 조회 실패: code={}", code, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
