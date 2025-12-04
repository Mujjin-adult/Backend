package com.incheon.notice.repository;

import com.incheon.notice.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 알림 설정 Repository
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    /**
     * 사용자의 모든 알림 설정 조회
     */
    List<UserPreference> findByUserId(Long userId);

    /**
     * 사용자의 활성화된 알림 설정 조회 (푸시 알림을 받을 카테고리)
     */
    @Query("SELECT up FROM UserPreference up WHERE up.user.id = :userId AND up.notificationEnabled = true")
    List<UserPreference> findActivePreferencesByUserId(@Param("userId") Long userId);

    /**
     * 특정 카테고리를 구독한 모든 사용자 조회 (알림 활성화된)
     */
    @Query("SELECT up FROM UserPreference up JOIN FETCH up.user WHERE up.category.id = :categoryId AND up.notificationEnabled = true")
    List<UserPreference> findActiveByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 사용자가 특정 카테고리를 구독했는지 확인
     */
    boolean existsByUserIdAndCategoryId(Long userId, Long categoryId);

    /**
     * 사용자와 카테고리로 설정 조회
     */
    Optional<UserPreference> findByUserIdAndCategoryId(Long userId, Long categoryId);

    /**
     * 사용자가 구독한 카테고리 ID 목록 조회 (알림 활성화된 것만)
     */
    @Query("SELECT up.category.id FROM UserPreference up WHERE up.user.id = :userId AND up.notificationEnabled = true")
    List<Long> findSubscribedCategoryIdsByUserId(@Param("userId") Long userId);
}
