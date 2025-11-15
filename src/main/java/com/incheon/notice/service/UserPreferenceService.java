package com.incheon.notice.service;

import com.incheon.notice.dto.CategoryDto;
import com.incheon.notice.dto.UserPreferenceDto;
import com.incheon.notice.entity.Category;
import com.incheon.notice.entity.User;
import com.incheon.notice.entity.UserPreference;
import com.incheon.notice.repository.CategoryRepository;
import com.incheon.notice.repository.UserPreferenceRepository;
import com.incheon.notice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    /**
     * 카테고리 구독 (알림 설정 생성)
     */
    @Transactional
    public UserPreferenceDto.Response subscribe(Long userId, UserPreferenceDto.SubscribeRequest request) {
        log.debug("카테고리 구독: userId={}, categoryId={}", userId, request.getCategoryId());

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 카테고리 조회
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다: " + request.getCategoryId()));

        // 이미 구독 중인지 확인
        if (userPreferenceRepository.existsByUserIdAndCategoryId(userId, request.getCategoryId())) {
            throw new RuntimeException("이미 구독 중인 카테고리입니다");
        }

        // 알림 설정 생성
        UserPreference preference = UserPreference.builder()
                .user(user)
                .category(category)
                .notificationEnabled(request.getNotificationEnabled())
                .build();

        UserPreference savedPreference = userPreferenceRepository.save(preference);

        return toResponse(savedPreference);
    }

    /**
     * 내 구독 카테고리 목록 조회
     */
    public List<UserPreferenceDto.Response> getMyPreferences(Long userId) {
        log.debug("내 구독 카테고리 목록 조회: userId={}", userId);

        List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);

        return preferences.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 알림 활성화된 구독 카테고리만 조회
     */
    public List<UserPreferenceDto.Response> getActivePreferences(Long userId) {
        log.debug("활성화된 구독 카테고리 조회: userId={}", userId);

        List<UserPreference> preferences = userPreferenceRepository.findActivePreferencesByUserId(userId);

        return preferences.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 특정 카테고리 구독 여부 확인
     */
    public boolean isSubscribed(Long userId, Long categoryId) {
        return userPreferenceRepository.existsByUserIdAndCategoryId(userId, categoryId);
    }

    /**
     * 알림 설정 변경 (활성화/비활성화)
     */
    @Transactional
    public UserPreferenceDto.Response updateNotification(Long userId, Long categoryId,
                                                         UserPreferenceDto.UpdateNotificationRequest request) {
        log.debug("알림 설정 변경: userId={}, categoryId={}, enabled={}",
                userId, categoryId, request.getNotificationEnabled());

        UserPreference preference = userPreferenceRepository.findByUserIdAndCategoryId(userId, categoryId)
                .orElseThrow(() -> new RuntimeException("구독하지 않은 카테고리입니다"));

        // 자신의 설정인지 확인
        if (!preference.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다");
        }

        // 알림 설정 변경
        preference.toggleNotification(request.getNotificationEnabled());

        return toResponse(preference);
    }

    /**
     * 카테고리 구독 취소
     */
    @Transactional
    public void unsubscribe(Long userId, Long categoryId) {
        log.debug("카테고리 구독 취소: userId={}, categoryId={}", userId, categoryId);

        UserPreference preference = userPreferenceRepository.findByUserIdAndCategoryId(userId, categoryId)
                .orElseThrow(() -> new RuntimeException("구독하지 않은 카테고리입니다"));

        // 자신의 설정인지 확인
        if (!preference.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다");
        }

        userPreferenceRepository.delete(preference);
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
