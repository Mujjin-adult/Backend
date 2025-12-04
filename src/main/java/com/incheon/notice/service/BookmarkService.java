package com.incheon.notice.service;

import com.incheon.notice.dto.BookmarkDto;
import com.incheon.notice.entity.Bookmark;
import com.incheon.notice.entity.CrawlNotice;
import com.incheon.notice.entity.User;
import com.incheon.notice.repository.BookmarkRepository;
import com.incheon.notice.repository.CrawlNoticeRepository;
import com.incheon.notice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 북마크 서비스
 * 사용자의 공지사항 북마크 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final CrawlNoticeRepository crawlNoticeRepository;
    private final UserRepository userRepository;

    /**
     * 북마크 생성
     */
    @Transactional
    public BookmarkDto.Response createBookmark(Long userId, BookmarkDto.CreateRequest request) {
        log.debug("북마크 생성: userId={}, noticeId={}", userId, request.getNoticeId());

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 공지사항 조회
        CrawlNotice notice = crawlNoticeRepository.findById(request.getNoticeId())
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다: " + request.getNoticeId()));

        // 이미 북마크되어 있는지 확인
        if (bookmarkRepository.existsByUserIdAndCrawlNoticeId(userId, request.getNoticeId())) {
            throw new RuntimeException("이미 북마크한 공지사항입니다");
        }

        // 북마크 생성
        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .crawlNotice(notice)
                .build();

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);

        return toResponse(savedBookmark);
    }

    /**
     * 북마크 삭제
     */
    @Transactional
    public void deleteBookmark(Long userId, Long bookmarkId) {
        log.debug("북마크 삭제: userId={}, bookmarkId={}", userId, bookmarkId);

        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new RuntimeException("북마크를 찾을 수 없습니다: " + bookmarkId));

        // 자신의 북마크인지 확인
        if (!bookmark.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다");
        }

        bookmarkRepository.delete(bookmark);
    }

    /**
     * 북마크 엔티티를 응답 DTO로 변환
     */
    private BookmarkDto.Response toResponse(Bookmark bookmark) {
        return BookmarkDto.Response.builder()
                .id(bookmark.getId())
                .noticeId(bookmark.getCrawlNotice().getId())
                .createdAt(bookmark.getCreatedAt())
                .build();
    }
}
