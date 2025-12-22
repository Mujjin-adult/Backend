package com.incheon.notice.service;

import com.incheon.notice.dto.DepartmentDto;
import com.incheon.notice.dto.UserDto;
import com.incheon.notice.entity.Department;
import com.incheon.notice.entity.User;
import com.incheon.notice.repository.DepartmentRepository;
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
    private final DepartmentRepository departmentRepository;
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
     * 사용자 시스템 알림 설정 수정
     */
    @Transactional
    public UserDto.Response updateSettings(Long userId, UserDto.UpdateSettingsRequest request) {
        log.debug("사용자 시스템 알림 설정 수정: userId={}, notification={}",
                userId, request.getSystemNotificationEnabled());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 시스템 알림 설정 업데이트
        user.updateSettings(request.getSystemNotificationEnabled());

        return toResponse(user);
    }

    /**
     * 사용자 이름 수정
     */
    @Transactional
    public UserDto.Response updateName(Long userId, UserDto.UpdateNameRequest request) {
        log.debug("사용자 이름 수정: userId={}, name={}", userId, request.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        user.updateName(request.getName());

        return toResponse(user);
    }

    /**
     * 사용자 학번 수정
     */
    @Transactional
    public UserDto.Response updateStudentId(Long userId, UserDto.UpdateStudentIdRequest request) {
        log.debug("사용자 학번 수정: userId={}, studentId={}", userId, request.getStudentId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 학번 중복 체크 (자신의 학번이 아닌 경우)
        if (!request.getStudentId().equals(user.getStudentId())
                && userRepository.existsByStudentId(request.getStudentId())) {
            throw new RuntimeException("이미 등록된 학번입니다");
        }

        user.updateStudentId(request.getStudentId());

        return toResponse(user);
    }

    /**
     * 사용자 학과 수정
     */
    @Transactional
    public UserDto.Response updateDepartment(Long userId, UserDto.UpdateDepartmentRequest request) {
        log.debug("사용자 학과 수정: userId={}, departmentId={}", userId, request.getDepartmentId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("학과를 찾을 수 없습니다: " + request.getDepartmentId()));

        user.updateDepartment(department);

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
    }

    /**
     * User 엔티티를 응답 DTO로 변환
     */
    private UserDto.Response toResponse(User user) {
        DepartmentDto.Response departmentResponse = null;
        if (user.getDepartment() != null) {
            departmentResponse = DepartmentDto.Response.from(user.getDepartment());
        }

        return UserDto.Response.builder()
                .id(user.getId())
                .studentId(user.getStudentId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .systemNotificationEnabled(user.getSystemNotificationEnabled())
                .department(departmentResponse)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
