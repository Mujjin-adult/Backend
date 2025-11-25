package com.incheon.notice.service;

import com.incheon.notice.dto.CategoryDto;
import com.incheon.notice.entity.Category;
import com.incheon.notice.repository.CategoryRepository;
import com.incheon.notice.repository.CrawlNoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카테고리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CrawlNoticeRepository crawlNoticeRepository;

    /**
     * 전체 카테고리 목록 조회
     */
    public List<CategoryDto.Response> getAllCategories() {
        log.debug("전체 카테고리 조회");

        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 활성 카테고리 목록 조회
     */
    public List<CategoryDto.Response> getActiveCategories() {
        log.debug("활성 카테고리 조회");

        List<Category> categories = categoryRepository.findByIsActiveTrue();

        return categories.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 카테고리 조회 (코드로)
     */
    public CategoryDto.Response getCategoryByCode(String code) {
        log.debug("카테고리 조회: code={}", code);

        Category category = categoryRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다: " + code));

        return toDto(category);
    }

    /**
     * 카테고리 ID로 조회
     */
    public CategoryDto.Response getCategoryById(Long id) {
        log.debug("카테고리 조회: id={}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다: " + id));

        return toDto(category);
    }

    /**
     * Entity -> DTO 변환
     */
    private CategoryDto.Response toDto(Category category) {
        // 해당 카테고리의 공지사항 개수 조회 (crawl_notice 테이블에서)
        long noticeCount = crawlNoticeRepository.countByCategoryId(category.getId());

        return CategoryDto.Response.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .type(category.getType().name())
                .url(category.getUrl())
                .isActive(category.getIsActive())
                .description(category.getDescription())
                .noticeCount((int) noticeCount)
                .build();
    }
}
