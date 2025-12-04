package com.incheon.notice.repository;

import com.incheon.notice.entity.UserDetailCategorySubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 상세 카테고리 구독 Repository
 */
@Repository
public interface UserDetailCategorySubscriptionRepository extends JpaRepository<UserDetailCategorySubscription, Long> {

    /**
     * 사용자와 상세 카테고리로 구독 조회
     */
    Optional<UserDetailCategorySubscription> findByUserIdAndDetailCategoryId(Long userId, Long detailCategoryId);

    /**
     * 사용자의 모든 구독 조회
     */
    List<UserDetailCategorySubscription> findByUserId(Long userId);

    /**
     * 사용자의 활성화된 구독만 조회
     */
    List<UserDetailCategorySubscription> findByUserIdAndSubscribedTrue(Long userId);

    /**
     * 사용자가 구독한 카테고리 이름 목록 조회
     */
    @Query("SELECT s.detailCategory.name FROM UserDetailCategorySubscription s WHERE s.user.id = :userId AND s.subscribed = true")
    List<String> findSubscribedCategoryNamesByUserId(@Param("userId") Long userId);

    /**
     * 특정 카테고리를 구독한 모든 사용자 ID 조회
     */
    @Query("SELECT s.user.id FROM UserDetailCategorySubscription s WHERE s.detailCategory.name = :categoryName AND s.subscribed = true")
    List<Long> findUserIdsBySubscribedCategoryName(@Param("categoryName") String categoryName);

    /**
     * 사용자가 특정 상세 카테고리를 구독했는지 확인
     */
    boolean existsByUserIdAndDetailCategoryIdAndSubscribedTrue(Long userId, Long detailCategoryId);

    /**
     * 사용자의 구독 삭제
     */
    void deleteByUserIdAndDetailCategoryId(Long userId, Long detailCategoryId);
}
