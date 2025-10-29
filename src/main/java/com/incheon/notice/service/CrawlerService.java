package com.incheon.notice.service;

import com.incheon.notice.dto.CrawlerDto;
import com.incheon.notice.entity.Category;
import com.incheon.notice.entity.Notice;
import com.incheon.notice.repository.CategoryRepository;
import com.incheon.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 크롤러 서비스
 * crawling-server로부터 받은 데이터를 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CrawlerService {

    private final NoticeRepository noticeRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 크롤링된 공지사항 저장
     * externalId를 기준으로 중복 체크
     *
     * @param request 크롤링된 공지사항 데이터
     * @return 저장 결과
     */
    public CrawlerDto.NoticeResponse saveNotice(CrawlerDto.NoticeRequest request) {
        log.info("크롤링 데이터 수신: title={}, externalId={}, categoryCode={}",
                request.getTitle(), request.getExternalId(), request.getCategoryCode());

        // 1. externalId로 중복 체크
        Optional<Notice> existingNotice = noticeRepository.findByExternalId(request.getExternalId());

        if (existingNotice.isPresent()) {
            // 기존 공지사항이 있으면 업데이트 (조회수 등)
            Notice notice = existingNotice.get();
            updateExistingNotice(notice, request);

            log.info("기존 공지사항 업데이트: id={}, externalId={}",
                    notice.getId(), notice.getExternalId());

            return CrawlerDto.NoticeResponse.builder()
                    .id(notice.getId())
                    .title(notice.getTitle())
                    .externalId(notice.getExternalId())
                    .categoryCode(request.getCategoryCode())
                    .created(false)
                    .message("기존 공지사항 업데이트됨")
                    .build();
        }

        // 2. 카테고리 조회 (없으면 예외)
        Category category = categoryRepository.findByCode(request.getCategoryCode())
                .orElseThrow(() -> new RuntimeException(
                        "카테고리를 찾을 수 없습니다: " + request.getCategoryCode()));

        // 3. 새 공지사항 생성
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent() != null ? request.getContent() : "")
                .url(request.getUrl())
                .externalId(request.getExternalId())
                .category(category)
                .author(request.getAuthor())
                .publishedAt(request.getPublishedAt() != null
                        ? request.getPublishedAt()
                        : LocalDateTime.now())
                .viewCount(request.getViewCount() != null ? request.getViewCount() : 0)
                .isImportant(request.getIsImportant() != null && request.getIsImportant())
                .isPinned(false)
                .attachments(request.getAttachments())
                .build();

        Notice savedNotice = noticeRepository.save(notice);

        log.info("새 공지사항 저장 완료: id={}, externalId={}, title={}",
                savedNotice.getId(), savedNotice.getExternalId(), savedNotice.getTitle());

        // 4. TODO: 푸시 알림 전송 (나중에 구현)
        // pushNotificationService.sendNewNoticeAlert(savedNotice);

        return CrawlerDto.NoticeResponse.builder()
                .id(savedNotice.getId())
                .title(savedNotice.getTitle())
                .externalId(savedNotice.getExternalId())
                .categoryCode(category.getCode())
                .created(true)
                .message("새 공지사항 저장됨")
                .build();
    }

    /**
     * 기존 공지사항 업데이트
     */
    private void updateExistingNotice(Notice notice, CrawlerDto.NoticeRequest request) {
        // 조회수가 증가했으면 업데이트
        if (request.getViewCount() != null && request.getViewCount() > notice.getViewCount()) {
            notice.updateViewCount(request.getViewCount());
        }

        // 내용이 변경되었으면 업데이트
        if (request.getContent() != null && !request.getContent().isEmpty() &&
                !request.getContent().equals(notice.getContent())) {
            // Notice 엔티티에 updateContent 메서드가 있다면 사용
            // notice.updateContent(request.getContent());
        }
    }

    /**
     * 특정 카테고리의 공지사항 개수 조회
     */
    @Transactional(readOnly = true)
    public long getNoticeCountByCategory(String categoryCode) {
        return noticeRepository.countByCategoryCode(categoryCode);
    }

    /**
     * 최근 크롤링된 공지사항 개수 조회
     */
    @Transactional(readOnly = true)
    public long getRecentNoticeCount(LocalDateTime since) {
        return noticeRepository.countByCreatedAtAfter(since);
    }
}
