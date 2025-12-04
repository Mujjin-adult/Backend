package com.incheon.notice.service;

import com.incheon.notice.dto.DetailCategoryDto;
import com.incheon.notice.entity.DetailCategory;
import com.incheon.notice.entity.User;
import com.incheon.notice.entity.UserDetailCategorySubscription;
import com.incheon.notice.repository.DetailCategoryRepository;
import com.incheon.notice.repository.UserDetailCategorySubscriptionRepository;
import com.incheon.notice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 알림 설정 서비스
 * detail_category 기반 카테고리 구독 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPreferenceService {

    private final UserDetailCategorySubscriptionRepository subscriptionRepository;
    private final DetailCategoryRepository detailCategoryRepository;
    private final UserRepository userRepository;

    /**
     * 전체 상세 카테고리 목록 조회 (사용자의 구독 여부 포함)
     * GET /api/preferences/detail-categories
     */
    public DetailCategoryDto.ListResponse getDetailCategoriesWithSubscription(Long userId) {
        log.debug("상세 카테고리 목록 조회 (구독 정보 포함): userId={}", userId);

        // 활성화된 모든 상세 카테고리 조회
        List<DetailCategory> allCategories = detailCategoryRepository.findByIsActiveTrueOrderByNameAsc();

        // 사용자의 구독 정보를 Map으로 변환 (categoryId -> subscribed)
        List<UserDetailCategorySubscription> userSubscriptions = subscriptionRepository.findByUserId(userId);
        Map<Long, Boolean> subscriptionMap = userSubscriptions.stream()
                .collect(Collectors.toMap(
                        sub -> sub.getDetailCategory().getId(),
                        UserDetailCategorySubscription::getSubscribed
                ));

        // 응답 생성
        List<DetailCategoryDto.Response> responses = allCategories.stream()
                .map(category -> DetailCategoryDto.Response.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .description(category.getDescription())
                        .isActive(category.getIsActive())
                        .subscribed(subscriptionMap.getOrDefault(category.getId(), false))
                        .build())
                .collect(Collectors.toList());

        return DetailCategoryDto.ListResponse.builder()
                .categories(responses)
                .build();
    }

    /**
     * 활성화된 구독 카테고리만 조회
     * GET /api/preferences/categories/active
     */
    public List<DetailCategoryDto.Response> getActiveSubscriptions(Long userId) {
        log.debug("활성화된 구독 카테고리 조회: userId={}", userId);

        List<UserDetailCategorySubscription> subscriptions = subscriptionRepository.findByUserIdAndSubscribedTrue(userId);

        return subscriptions.stream()
                .map(sub -> {
                    DetailCategory category = sub.getDetailCategory();
                    return DetailCategoryDto.Response.builder()
                            .id(category.getId())
                            .name(category.getName())
                            .description(category.getDescription())
                            .isActive(category.getIsActive())
                            .subscribed(true)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 상세 카테고리 구독 설정 일괄 변경
     * PATCH /api/preferences/detail-categories
     */
    @Transactional
    public DetailCategoryDto.ListResponse updateDetailCategorySubscriptions(
            Long userId,
            DetailCategoryDto.BatchSubscriptionRequest request) {

        log.debug("상세 카테고리 구독 일괄 변경: userId={}, count={}", userId, request.getSubscriptions().size());

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 각 구독 설정 처리
        for (DetailCategoryDto.SubscriptionRequest subRequest : request.getSubscriptions()) {
            Long categoryId = subRequest.getCategoryId();
            Boolean subscribed = subRequest.getSubscribed();

            // 상세 카테고리 존재 확인
            DetailCategory category = detailCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("상세 카테고리를 찾을 수 없습니다: " + categoryId));

            // 기존 구독 정보 조회
            Optional<UserDetailCategorySubscription> existingOpt =
                    subscriptionRepository.findByUserIdAndDetailCategoryId(userId, categoryId);

            if (existingOpt.isPresent()) {
                // 기존 구독이 있으면 업데이트
                UserDetailCategorySubscription existing = existingOpt.get();
                existing.updateSubscribed(subscribed);
                log.debug("구독 상태 업데이트: categoryId={}, subscribed={}", categoryId, subscribed);
            } else {
                // 없으면 새로 생성
                UserDetailCategorySubscription newSubscription = UserDetailCategorySubscription.builder()
                        .user(user)
                        .detailCategory(category)
                        .subscribed(subscribed)
                        .build();
                subscriptionRepository.save(newSubscription);
                log.debug("새 구독 생성: categoryId={}, subscribed={}", categoryId, subscribed);
            }
        }

        // 변경된 결과 반환
        return getDetailCategoriesWithSubscription(userId);
    }

    /**
     * 단일 카테고리 구독 토글
     */
    @Transactional
    public DetailCategoryDto.Response toggleSubscription(Long userId, Long detailCategoryId) {
        log.debug("카테고리 구독 토글: userId={}, detailCategoryId={}", userId, detailCategoryId);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 상세 카테고리 조회
        DetailCategory category = detailCategoryRepository.findById(detailCategoryId)
                .orElseThrow(() -> new RuntimeException("상세 카테고리를 찾을 수 없습니다: " + detailCategoryId));

        // 기존 구독 정보 조회
        Optional<UserDetailCategorySubscription> existingOpt =
                subscriptionRepository.findByUserIdAndDetailCategoryId(userId, detailCategoryId);

        Boolean newSubscribed;
        if (existingOpt.isPresent()) {
            // 기존 구독이 있으면 토글
            UserDetailCategorySubscription existing = existingOpt.get();
            existing.toggleSubscription();
            newSubscribed = existing.getSubscribed();
        } else {
            // 없으면 구독 상태로 새로 생성
            UserDetailCategorySubscription newSubscription = UserDetailCategorySubscription.builder()
                    .user(user)
                    .detailCategory(category)
                    .subscribed(true)
                    .build();
            subscriptionRepository.save(newSubscription);
            newSubscribed = true;
        }

        return DetailCategoryDto.Response.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .isActive(category.getIsActive())
                .subscribed(newSubscribed)
                .build();
    }

    /**
     * 특정 카테고리 구독 여부 확인
     */
    public boolean isSubscribed(Long userId, Long detailCategoryId) {
        return subscriptionRepository.existsByUserIdAndDetailCategoryIdAndSubscribedTrue(userId, detailCategoryId);
    }

    /**
     * 사용자가 구독한 카테고리 이름 목록 조회 (알림 발송 시 사용)
     */
    public List<String> getSubscribedCategoryNames(Long userId) {
        return subscriptionRepository.findSubscribedCategoryNamesByUserId(userId);
    }

    /**
     * 특정 카테고리를 구독한 사용자 ID 목록 조회 (푸시 알림 발송 시 사용)
     */
    public List<Long> getSubscriberUserIdsByCategoryName(String categoryName) {
        return subscriptionRepository.findUserIdsBySubscribedCategoryName(categoryName);
    }
}
