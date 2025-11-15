package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.AuthDto;
import com.incheon.notice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API Controller
 * 회원가입, 로그인, 토큰 갱신
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.incheon.notice.service.EmailVerificationService emailVerificationService;
    private final com.incheon.notice.service.PasswordResetService passwordResetService;

    /**
     * 회원가입
     * POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthDto.UserResponse>> signUp(
            @Valid @RequestBody AuthDto.SignUpRequest request) {
        AuthDto.UserResponse user = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다", user));
    }


    /**
     * 로그인
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }

    /**
     * 액세스 토큰 갱신
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        String newAccessToken = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("토큰 갱신 성공", newAccessToken));
    }

    /**
     * 로그아웃
     * POST /api/auth/logout
     *
     * Note: JWT는 stateless이므로 서버에서 토큰을 무효화할 수 없습니다.
     * 클라이언트에서 토큰을 삭제하면 됩니다.
     * 추후 Redis를 이용한 JWT 블랙리스트 기능을 추가할 예정입니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // TODO: JWT 블랙리스트 구현 (Phase 6.1)
        // 현재는 클라이언트에서 토큰을 삭제하도록 안내만 제공
        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다", null));
    }

    /**
     * 이메일 인증
     * GET /api/auth/verify-email?token={token}
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다", null));
    }

    /**
     * 인증 메일 재발송
     * POST /api/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(@RequestParam String email) {
        emailVerificationService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success("인증 메일이 재발송되었습니다", null));
    }

    /**
     * 비밀번호 찾기 (재설정 메일 발송)
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        passwordResetService.createAndSendResetToken(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("비밀번호 재설정 메일이 발송되었습니다", null));
    }

    /**
     * 비밀번호 재설정
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody AuthDto.ResetPasswordRequest request) {

        // 새 비밀번호와 확인 비밀번호 일치 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("새 비밀번호와 확인 비밀번호가 일치하지 않습니다"));
        }

        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 재설정되었습니다", null));
    }
}
