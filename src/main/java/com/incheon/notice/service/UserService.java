package com.incheon.notice.service;

import com.incheon.notice.dto.UserDto;
import com.incheon.notice.entity.User;
import com.incheon.notice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 서비스
 * 사용자 정보 조회 및 수정
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 정보 조회
     */
    public UserDto.Response getUserInfo(Long userId) {
        log.debug("사용자 정보 조회: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        return toResponse(user);
    }

    /**
     * 사용자 이름 수정
     */
    @Transactional
    public UserDto.Response updateName(Long userId, UserDto.UpdateNameRequest request) {
        log.debug("이름 수정: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        user.updateName(request.getName());

        return toResponse(user);
    }

    /**
     * 학번 수정
     */
    @Transactional
    public UserDto.Response updateStudentId(Long userId, UserDto.UpdateStudentIdRequest request) {
        log.debug("학번 수정: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 학번 중복 체크 (자신의 학번이 아닌 경우)
        if (request.getStudentId() != null && !request.getStudentId().equals(user.getStudentId())) {
            if (userRepository.existsByStudentId(request.getStudentId())) {
                throw new RuntimeException("이미 사용 중인 학번입니다");
            }
        }

        user.updateStudentId(request.getStudentId());

        return toResponse(user);
    }

    /**
     * 학과 수정
     */
    @Transactional
    public UserDto.Response updateDepartment(Long userId, UserDto.UpdateDepartmentRequest request) {
        log.debug("학과 수정: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        user.updateDepartment(request.getDepartment());

        return toResponse(user);
    }

    /**
     * 사용자 시스템 알림 설정 수정
     */
    @Transactional
    public UserDto.Response updateSettings(Long userId, UserDto.UpdateSettingsRequest request) {
        log.debug("시스템 알림 설정 수정: userId={}, notification={}",
                userId, request.getSystemNotificationEnabled());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        if (request.getSystemNotificationEnabled() != null) {
            user.updateSystemNotification(request.getSystemNotificationEnabled());
        }

        return toResponse(user);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long userId, UserDto.ChangePasswordRequest request) {
        log.debug("비밀번호 변경: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다");
        }

        // 새 비밀번호와 확인 비밀번호 일치 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다");
        }

        // 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.updatePassword(encodedPassword);
    }

    /**
     * FCM 토큰 업데이트
     */
    @Transactional
    public void updateFcmToken(Long userId, UserDto.UpdateFcmTokenRequest request) {
        log.debug("FCM 토큰 업데이트: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        user.updateFcmToken(request.getFcmToken());
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteAccount(Long userId, UserDto.DeleteAccountRequest request) {
        log.debug("회원 탈퇴: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 비밀번호 확인 (본인 확인)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        // 계정 비활성화 (실제 삭제 대신 비활성화)
        user.deactivate();

        // 완전 삭제를 원하는 경우 아래 주석 해제
        // userRepository.delete(user);
    }

    /**
     * User 엔티티를 응답 DTO로 변환
     */
    private UserDto.Response toResponse(User user) {
        return UserDto.Response.builder()
                .id(user.getId())
                .studentId(user.getStudentId())
                .email(user.getEmail())
                .name(user.getName())
                .department(user.getDepartment())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .systemNotificationEnabled(user.getSystemNotificationEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
