package com.incheon.notice.service;

import com.incheon.notice.dto.CategoryDto;
import com.incheon.notice.dto.NoticeDto;
import com.incheon.notice.entity.Category;
import com.incheon.notice.entity.Notice;
import com.incheon.notice.repository.CategoryRepository;
import com.incheon.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공지사항 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 전체 공지사항 페이징 조회
     */
    public Page<NoticeDto.Response> getAllNotices(Pageable pageable) {
        log.debug("전체 공지사항 조회: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Notice> noticePage = noticeRepository.findAllByOrderByPublishedAtDesc(pageable);

        return noticePage.map(this::toResponse);
    }

    /**
     * 공지사항 상세 조회
     */
    public NoticeDto.DetailResponse getNoticeById(Long id) {
        log.debug("공지사항 상세 조회: id={}", id);

        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다: " + id));

        return toDetailResponse(notice);
    }

    /**
     * 카테고리별 공지사항 조회
     */
    public Page<NoticeDto.Response> getNoticesByCategoryCode(String categoryCode, Pageable pageable) {
        log.debug("카테고리별 공지사항 조회: categoryCode={}, page={}, size={}",
                categoryCode, pageable.getPageNumber(), pageable.getPageSize());

        Category category = categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다: " + categoryCode));

        Page<Notice> noticePage = noticeRepository.findByCategoryOrderByPublishedAtDesc(category, pageable);

        return noticePage.map(this::toResponse);
    }

    /**
     * 공지사항 검색
     */
    public Page<NoticeDto.Response> searchNotices(String keyword, Pageable pageable) {
        log.debug("공지사항 검색: keyword={}, page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        Page<Notice> noticePage = noticeRepository.searchByKeyword(keyword, pageable);

        return noticePage.map(this::toResponse);
    }

    /**
     * Entity -> Response DTO 변환 (목록용)
     */
    private NoticeDto.Response toResponse(Notice notice) {
        return NoticeDto.Response.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .url(notice.getUrl())
                .categoryName(notice.getCategory().getName())
                .categoryCode(notice.getCategory().getCode())
                .author(notice.getAuthor())
                .publishedAt(notice.getPublishedAt())
                .viewCount(notice.getViewCount())
                .isImportant(notice.getIsImportant())
                .isPinned(notice.getIsPinned())
                .isBookmarked(false)  // TODO: 사용자 북마크 여부 확인 (인증 구현 후)
                .build();
    }

    /**
     * Entity -> DetailResponse DTO 변환 (상세용)
     */
    private NoticeDto.DetailResponse toDetailResponse(Notice notice) {
        CategoryDto.Response categoryDto = CategoryDto.Response.builder()
                .id(notice.getCategory().getId())
                .code(notice.getCategory().getCode())
                .name(notice.getCategory().getName())
                .type(notice.getCategory().getType().name())
                .url(notice.getCategory().getUrl())
                .isActive(notice.getCategory().getIsActive())
                .description(notice.getCategory().getDescription())
                .build();

        return NoticeDto.DetailResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .url(notice.getUrl())
                .externalId(notice.getExternalId())
                .category(categoryDto)
                .author(notice.getAuthor())
                .publishedAt(notice.getPublishedAt())
                .viewCount(notice.getViewCount())
                .isImportant(notice.getIsImportant())
                .isPinned(notice.getIsPinned())
                .attachments(notice.getAttachments())
                .isBookmarked(false)  // TODO: 사용자 북마크 여부 확인 (인증 구현 후)
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}
