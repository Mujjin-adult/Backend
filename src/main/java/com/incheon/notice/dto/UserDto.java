package com.incheon.notice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 관련 DTO
 */
public class UserDto {

    /**
     * 사용자 정보 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long id;
        private String studentId;
        private String email;
        private String name;
        private String department;
        private String role;
        private Boolean isActive;
        private Boolean systemNotificationEnabled;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 사용자 이름 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateNameRequest {

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        private String name;
    }

    /**
     * 사용자 학번 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStudentIdRequest {

        @NotBlank(message = "학번은 필수입니다")
        @Size(max = 20, message = "학번은 20자 이하여야 합니다")
        private String studentId;
    }

    /**
     * 사용자 학과 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateDepartmentRequest {

        @NotBlank(message = "학과는 필수입니다")
        @Size(max = 100, message = "학과는 100자 이하여야 합니다")
        private String department;
    }

    /**
     * 사용자 시스템 알림 설정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSettingsRequest {

        private Boolean systemNotificationEnabled;
    }

    /**
     * 비밀번호 변경 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {

        @NotBlank(message = "현재 비밀번호는 필수입니다")
        private String currentPassword;

        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다")
        private String newPassword;

        @NotBlank(message = "비밀번호 확인은 필수입니다")
        private String confirmPassword;
    }

    /**
     * FCM 토큰 업데이트 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateFcmTokenRequest {

        @NotBlank(message = "FCM 토큰은 필수입니다")
        private String fcmToken;
    }

    /**
     * 회원 탈퇴 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteAccountRequest {

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;  // 본인 확인용
    }
}
