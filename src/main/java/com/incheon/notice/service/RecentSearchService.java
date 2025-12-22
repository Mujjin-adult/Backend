package com.incheon.notice.service;

import com.incheon.notice.dto.RecentSearchDto;
import com.incheon.notice.entity.RecentSearch;
import com.incheon.notice.entity.User;
import com.incheon.notice.repository.RecentSearchRepository;
import com.incheon.notice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 최근 검색어 Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecentSearchService {

    private final RecentSearchRepository recentSearchRepository;
    private final UserRepository userRepository;

    private static final int MAX_RECENT_SEARCHES = 5;

    /**
     * 최근 검색어 저장
     * - 이미 존재하는 키워드면 검색 시각만 업데이트
     * - 5개 초과 시 가장 오래된 검색어 삭제
     */
    @Transactional
    public RecentSearchDto.Response saveRecentSearch(Long userId, RecentSearchDto.SaveRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String keyword = request.getKeyword().trim();

        // 이미 존재하는 검색어인지 확인
        var existingSearch = recentSearchRepository.findByUserAndKeyword(user, keyword);

        if (existingSearch.isPresent()) {
            // 기존 검색어 삭제 후 재생성 (검색 시각 갱신)
            recentSearchRepository.delete(existingSearch.get());
            recentSearchRepository.flush();
        } else {
            // 5개 초과 시 가장 오래된 검색어 삭제
            long count = recentSearchRepository.countByUser(user);
            if (count >= MAX_RECENT_SEARCHES) {
                List<RecentSearch> searches = recentSearchRepository.findTop5ByUserOrderByCreatedAtDesc(user);
                if (searches.size() == MAX_RECENT_SEARCHES) {
                    RecentSearch oldest = searches.get(MAX_RECENT_SEARCHES - 1);
                    recentSearchRepository.delete(oldest);
                    recentSearchRepository.flush();
                }
            }
        }

        // 새로운 검색어 저장
        RecentSearch newSearch = RecentSearch.builder()
                .user(user)
                .keyword(keyword)
                .build();

        RecentSearch saved = recentSearchRepository.save(newSearch);
        log.info("최근 검색어 저장 완료: userId={}, keyword={}", user.getId(), keyword);

        return RecentSearchDto.Response.from(saved);
    }

    /**
     * 최근 검색어 목록 조회 (최대 5개, 최신순)
     */
    public List<RecentSearchDto.Response> getRecentSearches(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<RecentSearch> searches = recentSearchRepository.findTop5ByUserOrderByCreatedAtDesc(user);

        return searches.stream()
                .map(RecentSearchDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 검색어 삭제
     */
    @Transactional
    public void deleteRecentSearch(Long userId, Long searchId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        RecentSearch search = recentSearchRepository.findById(searchId)
                .orElseThrow(() -> new IllegalArgumentException("검색어를 찾을 수 없습니다."));

        // 본인의 검색어인지 확인
        if (!search.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        recentSearchRepository.delete(search);
        log.info("최근 검색어 삭제 완료: userId={}, searchId={}", user.getId(), searchId);
    }

    /**
     * 모든 최근 검색어 삭제
     */
    @Transactional
    public void deleteAllRecentSearches(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        recentSearchRepository.deleteAllByUser(user);
        log.info("모든 최근 검색어 삭제 완료: userId={}", user.getId());
    }
}
