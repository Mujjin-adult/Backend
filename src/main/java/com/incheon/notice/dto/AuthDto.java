package com.incheon.notice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 인증 관련 DTO 모음
 */
public class AuthDto {

    /**
     * 회원가입 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignUpRequest {

        @NotBlank(message = "학번은 필수입니다")
        @Size(max = 20, message = "학번은 최대 20자입니다")
        private String studentId;

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 50, message = "비밀번호는 8~50자이어야 합니다")
        private String password;

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50, message = "이름은 최대 50자입니다")
        private String name;

        @NotBlank(message = "학과는 필수입니다")
        @Size(max = 100, message = "학과는 최대 100자입니다")
        private String department;  // 학과
    }

    /**
     * 이메일/비밀번호 로그인 요청 DTO (서버 로그인)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailLoginRequest {

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;

        private String fcmToken;  // 선택적: FCM 토큰
    }

    /**
     * 로그인 요청 DTO (Firebase Authentication 사용)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        @NotBlank(message = "Firebase ID Token은 필수입니다")
        private String idToken;  // Firebase에서 발급받은 ID Token

        private String fcmToken;  // 선택적: 로그인 시 FCM 토큰 업데이트
    }

    /**
     * 로그인 응답 DTO (Firebase Authentication 사용)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginResponse {

        private String idToken;  // Firebase ID Token (클라이언트가 이미 가지고 있음)
        @Builder.Default
        private String tokenType = "Bearer";
        private Long expiresIn;  // 토큰 만료 시간 (초 단위, 보통 3600초 = 1시간)
        private UserResponse user;
    }

    /**
     * 사용자 정보 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserResponse {

        private Long id;
        private String studentId;
        private String email;
        private String name;
        private String department;  // 학과
        private String role;
    }

    /**
     * 아이디 찾기 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FindIdRequest {

        @NotBlank(message = "이름은 필수입니다")
        private String name;

        @NotBlank(message = "학번은 필수입니다")
        private String studentId;
    }

    /**
     * 아이디 찾기 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FindIdResponse {

        private String maskedEmail;  // 마스킹된 이메일 (예: ch***@inu.ac.kr)
        private String message;
    }

    /**
     * 비밀번호 찾기 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForgotPasswordRequest {

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;
    }

    /**
     * 비밀번호 재설정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordRequest {

        @NotBlank(message = "토큰은 필수입니다")
        private String token;

        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, max = 50, message = "비밀번호는 8~50자이어야 합니다")
        private String newPassword;

        @NotBlank(message = "비밀번호 확인은 필수입니다")
        private String confirmPassword;
    }

    /**
     * Firebase UID 로그인 요청 DTO (테스트/개발용)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FirebaseUidLoginRequest {

        @NotBlank(message = "Firebase UID는 필수입니다")
        private String firebaseUid;

        private String fcmToken;  // 선택적: FCM 토큰
    }
}
