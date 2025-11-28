package com.incheon.notice.repository;

import com.incheon.notice.entity.Bookmark;
import com.incheon.notice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 북마크 Repository
 */
@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 사용자의 북마크 목록 조회 (페이징)
     */
    Page<Bookmark> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 사용자 ID로 북마크 목록 조회
     */
    @Query("SELECT b FROM Bookmark b JOIN FETCH b.crawlNotice WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    Page<Bookmark> findByUserIdWithNotice(@Param("userId") Long userId, Pageable pageable);

    /**
     * 사용자가 특정 공지사항을 북마크했는지 확인
     */
    boolean existsByUserIdAndCrawlNoticeId(Long userId, Long crawlNoticeId);

    /**
     * 사용자와 공지사항으로 북마크 조회
     */
    Optional<Bookmark> findByUserIdAndCrawlNoticeId(Long userId, Long crawlNoticeId);

    /**
     * 사용자의 북마크 개수
     */
    long countByUserId(Long userId);

    /**
     * 사용자 이메일과 공지사항 ID로 북마크 존재 여부 확인
     */
    boolean existsByUser_EmailAndCrawlNotice_Id(String email, Long crawlNoticeId);
}
