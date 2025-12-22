package com.incheon.notice.repository;

import com.incheon.notice.entity.RecentSearch;
import com.incheon.notice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 최근 검색어 Repository
 */
@Repository
public interface RecentSearchRepository extends JpaRepository<RecentSearch, Long> {

    /**
     * 사용자의 최근 검색어 목록 조회 (최신순, 최대 5개)
     */
    List<RecentSearch> findTop5ByUserOrderByCreatedAtDesc(User user);

    /**
     * 사용자의 특정 키워드 검색어 조회
     */
    Optional<RecentSearch> findByUserAndKeyword(User user, String keyword);

    /**
     * 사용자의 검색어 개수 조회
     */
    long countByUser(User user);

    /**
     * 사용자의 모든 검색어 삭제
     */
    void deleteAllByUser(User user);

    /**
     * 사용자의 특정 검색어 삭제
     */
    void deleteByUserAndId(User user, Long id);
}
