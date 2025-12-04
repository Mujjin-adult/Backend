package com.incheon.notice.service;

import com.incheon.notice.dto.NoticeDto;
import com.incheon.notice.entity.Bookmark;
import com.incheon.notice.entity.Category;
import com.incheon.notice.entity.CrawlNotice;
import com.incheon.notice.exception.NoticeNotFoundException;
import com.incheon.notice.repository.BookmarkRepository;
import com.incheon.notice.repository.CategoryRepository;
import com.incheon.notice.repository.CrawlNoticeRepository;
import com.incheon.notice.repository.UserDetailCategorySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 공지사항 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {

    private final CrawlNoticeRepository crawlNoticeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CategoryRepository categoryRepository;
    private final UserDetailCategorySubscriptionRepository detailCategorySubscriptionRepository;

    /**
     * 공지사항 목록 조회 (페이징, 필터링)
     *
     * @param categoryId 카테고리 ID (선택사항)
     * @param sortBy     정렬 방식 (latest, oldest, popular)
     * @param important  중요 공지만 조회 여부
     * @param pageable   페이징 정보
     * @param userEmail  현재 사용자 이메일 (북마크 상태 확인용)
     * @return 공지사항 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<NoticeDto.Response> getNotices(
            Long categoryId,
            String sortBy,
            Boolean important,
            Pageable pageable,
            String userEmail
    ) {
        log.info("Fetching notices - categoryId: {}, sortBy: {}, important: {}, page: {}",
                categoryId, sortBy, important, pageable.getPageNumber());

        // 동적 쿼리 생성 (Specification)
        Specification<CrawlNotice> spec = Specification.where(null);

        // 카테고리 필터
        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("categoryId"), categoryId));
        }

        // 중요 공지 필터
        if (important != null && important) {
            spec = spec.and((root, query, cb) ->
                    cb.isTrue(root.get("isImportant")));
        }

        // 정렬 옵션 적용
        Sort sort = getSortOrder(sortBy);
        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );

        // 공지사항 조회
        Page<CrawlNotice> noticesPage = crawlNoticeRepository.findAll(spec, pageableWithSort);

        // DTO로 변환
        return noticesPage.map(notice -> {
            NoticeDto.Response dto = NoticeDto.Response.from(notice);

            // 카테고리 정보 설정
            if (notice.getCategoryId() != null) {
                categoryRepository.findById(notice.getCategoryId()).ifPresent(category -> {
                    dto.setCategoryName(category.getName());
                    dto.setCategoryCode(category.getCode());
                });
            }

            // 북마크 상태 설정
            if (userEmail != null) {
                boolean isBookmarked = bookmarkRepository
                        .existsByUser_EmailAndCrawlNotice_Id(userEmail, notice.getId());
                dto.setBookmarked(isBookmarked);
            }

            return dto;
        });
    }

    /**
     * 공지사항 상세 조회 (조회수 증가)
     *
     * @param noticeId  공지사항 ID
     * @param userEmail 현재 사용자 이메일
     * @return 공지사항 상세 정보
     */
    @Transactional
    public NoticeDto.DetailResponse getNoticeDetail(Long noticeId, String userEmail) {
        log.info("Fetching notice detail - id: {}, user: {}", noticeId, userEmail);

        // 공지사항 조회
        CrawlNotice notice = crawlNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));

        // 조회수 증가
        notice.incrementViewCount();
        crawlNoticeRepository.save(notice);

        // DTO로 변환
        NoticeDto.DetailResponse response;

        if (notice.getCategoryId() != null) {
            Category category = categoryRepository.findById(notice.getCategoryId()).orElse(null);
            response = NoticeDto.DetailResponse.from(notice, category);
        } else {
            response = NoticeDto.DetailResponse.from(notice);
        }

        // 북마크 상태 설정
        if (userEmail != null) {
            boolean isBookmarked = bookmarkRepository
                    .existsByUser_EmailAndCrawlNotice_Id(userEmail, noticeId);
            response.setBookmarked(isBookmarked);
        }

        return response;
    }

    /**
     * 북마크한 공지사항 목록 조회
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 북마크한 공지사항 목록
     */
    @Transactional(readOnly = true)
    public Page<NoticeDto.Response> getBookmarkedNotices(Long userId, Pageable pageable) {
        log.info("Fetching bookmarked notices for user: {}", userId);

        // 사용자의 북마크 목록 조회
        Page<Bookmark> bookmarkPage = bookmarkRepository.findByUserIdWithNotice(userId, pageable);

        // DTO로 변환
        return bookmarkPage.map(bookmark -> {
            CrawlNotice notice = bookmark.getCrawlNotice();
            NoticeDto.Response dto = NoticeDto.Response.from(notice);

            // 카테고리 정보 설정
            if (notice.getCategoryId() != null) {
                categoryRepository.findById(notice.getCategoryId()).ifPresent(category -> {
                    dto.setCategoryName(category.getName());
                    dto.setCategoryCode(category.getCode());
                });
            }

            // 북마크 상태는 항상 true
            dto.setBookmarked(true);

            return dto;
        });
    }

    /**
     * 구독한 상세 카테고리의 공지사항 목록 조회
     * detail_category.name 값과 crawl_notice.category 필드를 매칭
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 구독 카테고리 공지사항 목록
     */
    @Transactional(readOnly = true)
    public Page<NoticeDto.Response> getSubscribedNotices(Long userId, Pageable pageable) {
        log.info("Fetching subscribed category notices for user: {}", userId);

        // 사용자가 구독한 상세 카테고리 이름 목록 조회
        List<String> subscribedCategoryNames = detailCategorySubscriptionRepository
                .findSubscribedCategoryNamesByUserId(userId);

        if (subscribedCategoryNames.isEmpty()) {
            log.info("No subscribed detail categories for user: {}", userId);
            return Page.empty(pageable);
        }

        log.debug("User {} subscribed categories: {}", userId, subscribedCategoryNames);

        // 해당 카테고리 이름의 공지사항 조회 (crawl_notice.category 필드와 매칭)
        Page<CrawlNotice> noticesPage = crawlNoticeRepository.findByCategoryIn(subscribedCategoryNames, pageable);

        // DTO로 변환
        return noticesPage.map(notice -> {
            NoticeDto.Response dto = NoticeDto.Response.from(notice);

            // 카테고리 정보 설정
            if (notice.getCategoryId() != null) {
                categoryRepository.findById(notice.getCategoryId()).ifPresent(category -> {
                    dto.setCategoryName(category.getName());
                    dto.setCategoryCode(category.getCode());
                });
            }

            // 북마크 상태 설정
            boolean isBookmarked = bookmarkRepository.existsByUserIdAndCrawlNoticeId(userId, notice.getId());
            dto.setBookmarked(isBookmarked);

            return dto;
        });
    }

    /**
     * 정렬 옵션에 따른 Sort 객체 생성
     *
     * @param sortBy 정렬 방식 (latest, oldest, popular)
     * @return Sort 객체
     */
    private Sort getSortOrder(String sortBy) {
        if (sortBy == null) {
            sortBy = "latest";
        }

        return switch (sortBy.toLowerCase()) {
            case "latest" -> Sort.by(Sort.Direction.DESC, "publishedAt")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "oldest" -> Sort.by(Sort.Direction.ASC, "publishedAt")
                    .and(Sort.by(Sort.Direction.ASC, "createdAt"));
            case "popular" -> Sort.by(Sort.Direction.DESC, "viewCount")
                    .and(Sort.by(Sort.Direction.DESC, "publishedAt"));
            default -> {
                log.warn("Unknown sort type: {}, using default (latest)", sortBy);
                yield Sort.by(Sort.Direction.DESC, "publishedAt")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            }
        };
    }
}
