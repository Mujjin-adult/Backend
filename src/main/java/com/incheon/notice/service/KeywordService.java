package com.incheon.notice.service;

import com.incheon.notice.dto.KeywordDto;
import com.incheon.notice.entity.Category;
import com.incheon.notice.entity.NotificationKeyword;
import com.incheon.notice.entity.User;
import com.incheon.notice.exception.BusinessException;
import com.incheon.notice.exception.DuplicateResourceException;
import com.incheon.notice.repository.CategoryRepository;
import com.incheon.notice.repository.NotificationKeywordRepository;
import com.incheon.notice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 키워드 서비스
 * 사용자의 알림 키워드 CRUD 및 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordService {

    private final NotificationKeywordRepository keywordRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 최대 키워드 등록 개수
     */
    private static final int MAX_KEYWORDS_PER_USER = 10;

    /**
     * 사용자의 키워드 목록 조회
     *
     * @param userId 사용자 ID
     * @return 키워드 목록
     */
    @Transactional(readOnly = true)
    public List<KeywordDto.Response> getMyKeywords(Long userId) {
        List<NotificationKeyword> keywords = keywordRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return keywords.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 키워드 추가
     * - 알림은 기본적으로 활성화 상태로 등록됨 (isActive = true)
     *
     * @param userId 사용자 ID
     * @param request 키워드 생성 요청
     * @return 생성된 키워드
     */
    @Transactional
    public KeywordDto.Response createKeyword(Long userId, KeywordDto.CreateRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다"));

        // 2. 키워드 전처리 (공백 제거, # 제거)
        String keyword = normalizeKeyword(request.getKeyword());

        // 3. 중복 체크
        if (keywordRepository.existsByUserIdAndKeyword(userId, keyword)) {
            throw new DuplicateResourceException("이미 등록된 키워드입니다: " + keyword);
        }

        // 4. 최대 개수 체크
        long currentCount = keywordRepository.countByUserId(userId);
        if (currentCount >= MAX_KEYWORDS_PER_USER) {
            throw new BusinessException("키워드는 최대 " + MAX_KEYWORDS_PER_USER + "개까지 등록할 수 있습니다");
        }

        // 5. 키워드 생성 (isActive = true 기본값)
        NotificationKeyword notificationKeyword = NotificationKeyword.builder()
                .user(user)
                .keyword(keyword)
                .isActive(true)  // 기본 활성화
                .matchedCount(0)
                .build();

        NotificationKeyword saved = keywordRepository.save(notificationKeyword);

        log.info("키워드 등록 완료: userId={}, keyword={}", userId, keyword);

        return toResponse(saved);
    }

    /**
     * 키워드 삭제
     *
     * @param userId 사용자 ID
     * @param keywordId 키워드 ID
     */
    @Transactional
    public void deleteKeyword(Long userId, Long keywordId) {
        NotificationKeyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new BusinessException("키워드를 찾을 수 없습니다"));

        // 본인 키워드인지 확인
        if (!keyword.getUser().getId().equals(userId)) {
            throw new BusinessException("삭제 권한이 없습니다");
        }

        keywordRepository.delete(keyword);

        log.info("키워드 삭제 완료: userId={}, keywordId={}, keyword={}",
                userId, keywordId, keyword.getKeyword());
    }

    /**
     * 키워드 활성화/비활성화 토글
     *
     * @param userId 사용자 ID
     * @param keywordId 키워드 ID
     * @return 토글된 키워드
     */
    @Transactional
    public KeywordDto.Response toggleKeyword(Long userId, Long keywordId) {
        NotificationKeyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new BusinessException("키워드를 찾을 수 없습니다"));

        // 본인 키워드인지 확인
        if (!keyword.getUser().getId().equals(userId)) {
            throw new BusinessException("수정 권한이 없습니다");
        }

        // 토글
        keyword.toggleActive();
        NotificationKeyword saved = keywordRepository.save(keyword);

        log.info("키워드 토글 완료: userId={}, keywordId={}, keyword={}, isActive={}",
                userId, keywordId, keyword.getKeyword(), saved.getIsActive());

        return toResponse(saved);
    }

    /**
     * 키워드 정규화 (# 제거, 공백 정리)
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        return keyword
                .replace("#", "")
                .trim()
                .replaceAll("\\s+", " ");  // 연속 공백을 하나로
    }

    /**
     * Entity -> Response DTO 변환
     */
    private KeywordDto.Response toResponse(NotificationKeyword keyword) {
        String categoryName = null;

        // 카테고리 이름 조회
        if (keyword.getCategoryId() != null) {
            categoryName = categoryRepository.findById(keyword.getCategoryId())
                    .map(Category::getName)
                    .orElse(null);
        }

        return KeywordDto.Response.builder()
                .id(keyword.getId())
                .keyword(keyword.getKeyword())
                .categoryId(keyword.getCategoryId())
                .categoryName(categoryName)
                .isActive(keyword.getIsActive())
                .matchedCount(keyword.getMatchedCount())
                .lastNotifiedAt(keyword.getLastNotifiedAt())
                .createdAt(keyword.getCreatedAt())
                .build();
    }
}
