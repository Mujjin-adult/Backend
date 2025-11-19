package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.UserDto;
import com.incheon.notice.security.CustomUserDetailsService;
import com.incheon.notice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 API Controller
 * 사용자 정보 조회 및 수정, 설정 관리
 */
@Tag(name = "사용자 정보", description = "사용자 프로필 조회 및 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     * GET /api/users/me
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.Response>> getMyInfo() {
        Long userId = getCurrentUserId();
        UserDto.Response userInfo = userService.getUserInfo(userId);

        return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회 성공", userInfo));
    }

    /**
     * 프로필 수정
     * PUT /api/users/me
     */
    @Operation(summary = "프로필 수정", description = "사용자의 이름 등 프로필 정보를 수정합니다.")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.Response>> updateProfile(
            @Valid @RequestBody UserDto.UpdateProfileRequest request) {

        Long userId = getCurrentUserId();
        UserDto.Response userInfo = userService.updateProfile(userId, request);

        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다", userInfo));
    }

    /**
     * 사용자 설정 수정 (다크 모드, 시스템 알림)
     * PUT /api/users/settings
     */
    @Operation(summary = "사용자 설정 수정", description = "다크 모드, 시스템 알림 등 사용자 설정을 변경합니다.")
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<UserDto.Response>> updateSettings(
            @Valid @RequestBody UserDto.UpdateSettingsRequest request) {

        Long userId = getCurrentUserId();
        UserDto.Response userInfo = userService.updateSettings(userId, request);

        return ResponseEntity.ok(ApiResponse.success("설정이 변경되었습니다", userInfo));
    }

    /**
     * 비밀번호 변경
     * PUT /api/users/password
     */
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새로운 비밀번호로 변경합니다.")
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody UserDto.ChangePasswordRequest request) {

        Long userId = getCurrentUserId();
        userService.changePassword(userId, request);

        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다", null));
    }

    /**
     * FCM 토큰 업데이트
     * PUT /api/users/fcm-token
     */
    @Operation(summary = "FCM 토큰 업데이트", description = "푸시 알림을 위한 FCM 토큰을 업데이트합니다.")
    @PutMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(
            @Valid @RequestBody UserDto.UpdateFcmTokenRequest request) {

        Long userId = getCurrentUserId();
        userService.updateFcmToken(userId, request);

        return ResponseEntity.ok(ApiResponse.success("FCM 토큰이 업데이트되었습니다", null));
    }

    /**
     * 회원 탈퇴
     * DELETE /api/users/me
     */
    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 처리합니다. 모든 사용자 데이터가 삭제됩니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @Valid @RequestBody UserDto.DeleteAccountRequest request) {

        Long userId = getCurrentUserId();
        userService.deleteAccount(userId, request);

        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다", null));
    }

    /**
     * SecurityContext에서 현재 인증된 사용자 ID 가져오기
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증이 필요합니다");
        }

        return ((CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal()).getUserId();
    }
}
