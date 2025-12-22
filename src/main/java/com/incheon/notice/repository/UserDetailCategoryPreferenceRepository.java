package com.incheon.notice.repository;

import com.incheon.notice.entity.UserDetailCategoryPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 상세 카테고리 구독 Repository
 */
@Repository
public interface UserDetailCategoryPreferenceRepository extends JpaRepository<UserDetailCategoryPreference, Long> {

    /**
     * 사용자의 모든 상세 카테고리 구독 설정 조회
     */
    List<UserDetailCategoryPreference> findByUserId(Long userId);

    /**
     * 사용자의 특정 상세 카테고리 구독 설정 조회
     */
    Optional<UserDetailCategoryPreference> findByUserIdAndDetailCategoryId(Long userId, Long detailCategoryId);

    /**
     * 사용자의 활성화된 구독 목록 조회
     */
    List<UserDetailCategoryPreference> findByUserIdAndEnabledTrue(Long userId);

    /**
     * 사용자의 상세 카테고리 구독 상태 업데이트
     */
    @Modifying
    @Query("UPDATE UserDetailCategoryPreference p SET p.enabled = :enabled WHERE p.user.id = :userId AND p.detailCategory.id = :detailCategoryId")
    int updateEnabled(@Param("userId") Long userId, @Param("detailCategoryId") Long detailCategoryId, @Param("enabled") Boolean enabled);

    /**
     * 사용자의 모든 상세 카테고리 구독 삭제
     */
    void deleteByUserId(Long userId);

    /**
     * 특정 상세 카테고리를 구독 중인 사용자 수 조회
     */
    long countByDetailCategoryIdAndEnabledTrue(Long detailCategoryId);
}
