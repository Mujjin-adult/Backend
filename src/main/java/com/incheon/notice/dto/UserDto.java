package com.incheon.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
        private String role;
        private Boolean isActive;
        private Boolean systemNotificationEnabled;
        private DepartmentDto.Response department;  // 소속 학과 정보
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 사용자 시스템 알림 설정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "푸시 알림 전체 설정 요청")
    public static class UpdateSettingsRequest {

        @Schema(
                description = "푸시 알림 전체 활성화 여부 (true: 모든 알림 받기, false: 모든 알림 끄기)",
                example = "true"
        )
        private Boolean systemNotificationEnabled;
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

        @NotNull(message = "학과 ID는 필수입니다")
        private Long departmentId;
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
