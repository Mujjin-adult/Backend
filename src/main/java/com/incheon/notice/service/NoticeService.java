package com.incheon.notice.service;

import com.incheon.notice.dto.NoticeDto;
import com.incheon.notice.entity.Bookmark;
import com.incheon.notice.entity.Category;
import com.incheon.notice.entity.CrawlNotice;
import com.incheon.notice.entity.UserDetailCategoryPreference;
import com.incheon.notice.exception.NoticeNotFoundException;
import com.incheon.notice.repository.BookmarkRepository;
import com.incheon.notice.repository.CategoryRepository;
import com.incheon.notice.repository.CrawlNoticeRepository;
import com.incheon.notice.repository.UserDetailCategoryPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    private final CategoryService categoryService;
    private final UserDetailCategoryPreferenceRepository userDetailCategoryPreferenceRepository;

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

        // 배치 조회를 위한 ID 목록 추출
        List<CrawlNotice> notices = noticesPage.getContent();
        List<Long> noticeIds = notices.stream()
                .map(CrawlNotice::getId)
                .toList();

        // 카테고리 정보 배치 조회 (캐싱 적용됨)
        Map<Long, Category> categoryMap = categoryService.getCategoryMap();

        // 북마크 정보 배치 조회 (로그인 사용자만)
        Set<Long> bookmarkedNoticeIds = Collections.emptySet();
        if (userEmail != null && !noticeIds.isEmpty()) {
            bookmarkedNoticeIds = bookmarkRepository.findBookmarkedNoticeIdsByUserEmail(userEmail, noticeIds);
        }

        // DTO로 변환 (배치 조회 결과 활용)
        final Set<Long> finalBookmarkedIds = bookmarkedNoticeIds;
        return noticesPage.map(notice -> {
            NoticeDto.Response dto = NoticeDto.Response.from(notice);

            // 카테고리 정보 설정 (Map에서 O(1) 조회)
            if (notice.getCategoryId() != null) {
                Category category = categoryMap.get(notice.getCategoryId());
                if (category != null) {
                    dto.setCategoryName(category.getName());
                    dto.setCategoryCode(category.getCode());
                }
            }

            // 북마크 상태 설정 (Set에서 O(1) 조회)
            dto.setBookmarked(finalBookmarkedIds.contains(notice.getId()));

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
        Category category = findCategoryForNotice(notice);
        NoticeDto.DetailResponse response;

        if (category != null) {
            response = NoticeDto.DetailResponse.from(notice, category);
            response.setSource(category.getName());  // source도 카테고리 name으로 설정
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
     * 북마크한 공지사항 조회
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 북마크한 공지사항 목록
     */
    @Transactional(readOnly = true)
    public Page<NoticeDto.Response> getBookmarkedNotices(Long userId, Pageable pageable) {
        log.info("Fetching bookmarked notices for user: {}", userId);

        Page<Bookmark> bookmarks = bookmarkRepository.findByUserIdWithNotice(userId, pageable);

        // 카테고리 정보 배치 조회 (캐싱 적용됨)
        Map<Long, Category> categoryMap = categoryService.getCategoryMap();

        return bookmarks.map(bookmark -> {
            CrawlNotice notice = bookmark.getCrawlNotice();
            NoticeDto.Response dto = NoticeDto.Response.from(notice);
            dto.setBookmarked(true);

            // 카테고리 정보 설정 (Map에서 O(1) 조회)
            if (notice.getCategoryId() != null) {
                Category category = categoryMap.get(notice.getCategoryId());
                if (category != null) {
                    dto.setCategoryName(category.getName());
                    dto.setCategoryCode(category.getCode());
                }
            }

            return dto;
        });
    }

    /**
     * 구독한 카테고리의 공지사항 조회
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 구독한 카테고리의 공지사항 목록
     */
    @Transactional(readOnly = true)
    public Page<NoticeDto.Response> getSubscribedNotices(Long userId, Pageable pageable) {
        log.info("Fetching subscribed notices for user: {}", userId);

        // 사용자가 구독한 상세 카테고리 목록 조회
        List<String> subscribedCategories = userDetailCategoryPreferenceRepository
                .findByUserIdAndEnabledTrue(userId)
                .stream()
                .map(pref -> pref.getDetailCategory().getName())
                .collect(Collectors.toList());

        if (subscribedCategories.isEmpty()) {
            log.info("User {} has no subscribed categories", userId);
            return Page.empty(pageable);
        }

        // 해당 카테고리의 공지사항 조회
        Page<CrawlNotice> notices = crawlNoticeRepository.findByCategoryIn(subscribedCategories, pageable);

        // 배치 조회를 위한 ID 목록 추출
        List<Long> noticeIds = notices.getContent().stream()
                .map(CrawlNotice::getId)
                .toList();

        // 카테고리 정보 배치 조회 (캐싱 적용됨)
        Map<Long, Category> categoryMap = categoryService.getCategoryMap();

        // 북마크 정보 배치 조회
        Set<Long> bookmarkedNoticeIds = Collections.emptySet();
        if (!noticeIds.isEmpty()) {
            bookmarkedNoticeIds = bookmarkRepository.findBookmarkedNoticeIdsByUserId(userId, noticeIds);
        }

        final Set<Long> finalBookmarkedIds = bookmarkedNoticeIds;
        return notices.map(notice -> {
            NoticeDto.Response dto = NoticeDto.Response.from(notice);

            // 북마크 상태 설정 (Set에서 O(1) 조회)
            dto.setBookmarked(finalBookmarkedIds.contains(notice.getId()));

            // 카테고리 정보 설정 (Map에서 O(1) 조회)
            if (notice.getCategoryId() != null) {
                Category category = categoryMap.get(notice.getCategoryId());
                if (category != null) {
                    dto.setCategoryName(category.getName());
                    dto.setCategoryCode(category.getCode());
                }
            }

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

    /**
     * 공지사항에 해당하는 카테고리 조회
     * 1. categoryId가 있으면 해당 ID로 조회
     * 2. categoryId가 없고 source가 있으면 source를 code로 사용하여 조회
     *
     * @param notice 공지사항
     * @return 카테고리 (없으면 null)
     */
    private Category findCategoryForNotice(CrawlNotice notice) {
        // 1. categoryId로 조회
        if (notice.getCategoryId() != null) {
            return categoryRepository.findById(notice.getCategoryId()).orElse(null);
        }

        // 2. source를 code로 사용하여 조회
        if (notice.getSource() != null && !notice.getSource().isEmpty()) {
            return categoryRepository.findByCode(notice.getSource()).orElse(null);
        }

        return null;
    }
}
