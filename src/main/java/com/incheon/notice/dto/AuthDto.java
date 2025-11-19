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
    }

    /**
     * 로그인 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;

        private String fcmToken;  // 선택적: 로그인 시 FCM 토큰 업데이트
    }

    /**
     * 로그인 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginResponse {

        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn;  // 토큰 만료 시간 (초 단위)
        private UserResponse user;
    }

    /**
     * 토큰 갱신 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenRequest {

        @NotBlank(message = "리프레시 토큰은 필수입니다")
        private String refreshToken;
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
}
