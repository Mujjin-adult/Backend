package com.incheon.notice.service;

import com.incheon.notice.dto.CategoryDto;
import com.incheon.notice.dto.UserPreferenceDto;
import com.incheon.notice.entity.Category;
import com.incheon.notice.entity.DetailCategory;
import com.incheon.notice.entity.User;
import com.incheon.notice.entity.UserDetailCategoryPreference;
import com.incheon.notice.entity.UserPreference;
import com.incheon.notice.repository.CategoryRepository;
import com.incheon.notice.repository.DetailCategoryRepository;
import com.incheon.notice.repository.UserDetailCategoryPreferenceRepository;
import com.incheon.notice.repository.UserPreferenceRepository;
import com.incheon.notice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 알림 설정 서비스
 * 사용자가 구독한 카테고리 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final DetailCategoryRepository detailCategoryRepository;
    private final UserDetailCategoryPreferenceRepository userDetailCategoryPreferenceRepository;

    /**
     * 전체 상세 카테고리와 사용자 구독 상태 조회
     */
    public List<UserPreferenceDto.DetailCategoryResponse> getDetailCategoriesWithSubscriptionStatus(Long userId) {
        log.debug("상세 카테고리 목록 조회 (구독 상태 포함): userId={}", userId);

        // 전체 상세 카테고리 조회
        List<DetailCategory> allCategories = detailCategoryRepository.findAllByOrderByNameAsc();

        // 사용자의 구독 설정 조회
        List<UserDetailCategoryPreference> userPrefs = userDetailCategoryPreferenceRepository.findByUserId(userId);

        // 구독 상태 맵 생성
        Map<Long, Boolean> subscriptionMap = userPrefs.stream()
                .collect(Collectors.toMap(
                        p -> p.getDetailCategory().getId(),
                        UserDetailCategoryPreference::getEnabled
                ));

        // DTO 변환
        return allCategories.stream()
                .map(cat -> UserPreferenceDto.DetailCategoryResponse.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .subscribed(subscriptionMap.getOrDefault(cat.getId(), false))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 상세 카테고리 구독 설정 일괄 업데이트
     */
    @Transactional
    public List<UserPreferenceDto.DetailCategoryResponse> updateDetailCategorySubscriptions(
            Long userId, UserPreferenceDto.DetailCategorySubscribeRequest request) {
        log.debug("상세 카테고리 구독 설정 업데이트: userId={}, count={}", userId, request.getSubscriptions().size());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        for (UserPreferenceDto.DetailCategorySubscription subscription : request.getSubscriptions()) {
            DetailCategory category = detailCategoryRepository.findById(subscription.getDetailCategoryId())
                    .orElseThrow(() -> new RuntimeException("상세 카테고리를 찾을 수 없습니다: " + subscription.getDetailCategoryId()));

            Optional<UserDetailCategoryPreference> existing =
                    userDetailCategoryPreferenceRepository.findByUserIdAndDetailCategoryId(userId, subscription.getDetailCategoryId());

            if (existing.isPresent()) {
                // 기존 설정 업데이트
                existing.get().updateEnabled(subscription.getEnabled());
            } else {
                // 새 설정 생성
                UserDetailCategoryPreference newPref = UserDetailCategoryPreference.builder()
                        .user(user)
                        .detailCategory(category)
                        .enabled(subscription.getEnabled())
                        .build();
                userDetailCategoryPreferenceRepository.save(newPref);
            }
        }

        return getDetailCategoriesWithSubscriptionStatus(userId);
    }

    /**
     * 알림 활성화된 구독 카테고리만 조회 (기존 카테고리 기반)
     */
    public List<UserPreferenceDto.Response> getActivePreferences(Long userId) {
        log.debug("활성화된 구독 카테고리 조회: userId={}", userId);

        List<UserPreference> preferences = userPreferenceRepository.findActivePreferencesByUserId(userId);

        return preferences.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 특정 카테고리를 구독하는 모든 사용자 조회 (알림 활성화된)
     * 푸시 알림 발송 시 사용
     */
    public List<UserPreference> getSubscribersByCategory(Long categoryId) {
        log.debug("카테고리 구독자 조회: categoryId={}", categoryId);

        return userPreferenceRepository.findActiveByCategoryId(categoryId);
    }

    /**
     * UserPreference 엔티티를 응답 DTO로 변환
     */
    private UserPreferenceDto.Response toResponse(UserPreference preference) {
        Category category = preference.getCategory();

        return UserPreferenceDto.Response.builder()
                .id(preference.getId())
                .category(CategoryDto.Response.builder()
                        .id(category.getId())
                        .code(category.getCode())
                        .name(category.getName())
                        .type(category.getType().name())
                        .url(category.getUrl())
                        .isActive(category.getIsActive())
                        .description(category.getDescription())
                        .build())
                .notificationEnabled(preference.getNotificationEnabled())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
