package com.incheon.notice.controller;

import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.dto.AuthDto;
import com.incheon.notice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API Controller
 * 회원가입, 로그인, 토큰 갱신
 */
@Tag(name = "인증 및 회원관리", description = "회원가입, 로그인, 아이디/비밀번호 찾기 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.incheon.notice.service.EmailVerificationService emailVerificationService;
    private final com.incheon.notice.service.PasswordResetService passwordResetService;

    /**
     * 회원가입 (레거시 - Firebase SDK 사용 권장)
     * POST /api/auth/signup
     *
     * ⚠️ 주의: Firebase Authentication 사용을 권장합니다.
     * 클라이언트에서 Firebase SDK의 createUserWithEmailAndPassword()를 사용하고,
     * 발급받은 ID Token으로 /api/auth/login을 호출하면 자동으로 회원가입됩니다.
     *
     * 이 엔드포인트는 하위 호환성을 위해 유지되며, Firebase를 사용하지 않는 경우에만 사용하세요.
     */
    @Operation(
        summary = "회원가입 (레거시)",
        description = """
            ⚠️ **Firebase SDK 사용을 권장합니다**

            **권장 방법 (Firebase):**
            1. 클라이언트에서 Firebase SDK로 회원가입: `createUserWithEmailAndPassword(email, password)`
            2. Firebase ID Token 발급받기: `user.getIdToken()`
            3. `/api/auth/login`에 ID Token 전송
            4. 서버에서 자동으로 사용자 생성

            **레거시 방법 (이 API):**
            인천대학교 이메일로 직접 회원가입을 진행합니다. 가입 후 이메일 인증이 필요합니다.
            Firebase를 사용하지 않는 특수한 경우에만 사용하세요.
            """
    )
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthDto.UserResponse>> signUp(
            @Valid @RequestBody AuthDto.SignUpRequest request) {
        AuthDto.UserResponse user = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다", user));
    }


    /**
     * 로그인 (Firebase Authentication)
     * POST /api/auth/login
     *
     * Firebase SDK로 로그인 후 발급받은 ID Token을 전송하여 인증합니다.
     * 서버에 사용자 정보가 없는 경우 자동으로 회원가입됩니다.
     */
    @Operation(
        summary = "로그인 (Firebase Authentication)",
        description = """
            Firebase ID Token을 사용하여 로그인합니다.

            **사용 방법:**
            1. 클라이언트에서 Firebase SDK로 로그인
               - 이메일/비밀번호: `signInWithEmailAndPassword(email, password)`
               - Google: `signInWithPopup(googleProvider)`
               - 기타 소셜 로그인
            2. Firebase ID Token 발급: `user.getIdToken()`
            3. 이 API에 ID Token 전송
            4. 서버에서 토큰 검증 및 사용자 정보 동기화

            **자동 회원가입:**
            Firebase로 로그인한 사용자가 서버 DB에 없는 경우, 자동으로 사용자가 생성됩니다.

            **토큰 갱신:**
            Firebase SDK가 자동으로 처리합니다. `user.getIdToken(true)`를 호출하세요.
            """
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }

    /**
     * 로그아웃
     * POST /api/auth/logout
     *
     * Note: Firebase Authentication 사용 시 클라이언트에서 Firebase SDK의 signOut()을 호출하면 됩니다.
     * 서버에서는 별도 처리가 필요 없습니다.
     */
    @Operation(summary = "로그아웃", description = "로그아웃 처리를 합니다. Firebase SDK에서 auth().signOut()을 호출하세요.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // Firebase Authentication 사용 시 클라이언트에서 처리
        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다. 클라이언트에서 Firebase signOut()을 호출하세요.", null));
    }

    /**
     * 이메일 인증 (레거시 - Firebase SDK 사용 권장)
     * GET /api/auth/verify-email?token={token}
     *
     * ⚠️ 주의: Firebase를 사용하는 경우, Firebase SDK의 이메일 인증 기능을 사용하세요.
     * 클라이언트에서 user.sendEmailVerification()을 호출하면 Firebase가 자동으로 인증 메일을 발송합니다.
     *
     * 이 엔드포인트는 레거시 회원가입(/api/auth/signup)을 사용한 경우에만 필요합니다.
     */
    @Operation(
        summary = "이메일 인증 (레거시)",
        description = """
            ⚠️ **Firebase SDK 사용을 권장합니다**

            **권장 방법 (Firebase):**
            ```javascript
            // 회원가입 후
            await user.sendEmailVerification();
            // 사용자가 이메일 링크를 클릭하면 Firebase가 자동으로 인증 처리
            ```

            **레거시 방법 (이 API):**
            레거시 회원가입 API(/api/auth/signup)를 사용한 경우, 발송된 이메일의 인증 링크로 이메일을 인증합니다.
            """
    )
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다", null));
    }

    /**
     * 인증 메일 재발송 (레거시 - Firebase SDK 사용 권장)
     * POST /api/auth/resend-verification
     *
     * ⚠️ 주의: Firebase를 사용하는 경우, Firebase SDK의 이메일 인증 기능을 사용하세요.
     *
     * 이 엔드포인트는 레거시 회원가입(/api/auth/signup)을 사용한 경우에만 필요합니다.
     */
    @Operation(
        summary = "인증 메일 재발송 (레거시)",
        description = """
            ⚠️ **Firebase SDK 사용을 권장합니다**

            **권장 방법 (Firebase):**
            ```javascript
            const user = auth().currentUser;
            await user.sendEmailVerification();
            ```

            **레거시 방법 (이 API):**
            레거시 회원가입을 사용한 경우, 이메일 인증 메일을 다시 발송합니다.
            """
    )
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(@RequestParam String email) {
        emailVerificationService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success("인증 메일이 재발송되었습니다", null));
    }

    /**
     * 아이디 찾기 (이름, 학번으로 이메일 찾기)
     * POST /api/auth/find-id
     */
    @Operation(summary = "아이디 찾기", description = "이름과 학번으로 아이디(이메일)를 찾습니다. 마스킹된 이메일과 함께 전체 이메일이 발송됩니다.")
    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<AuthDto.FindIdResponse>> findId(
            @Valid @RequestBody AuthDto.FindIdRequest request) {
        AuthDto.FindIdResponse response = authService.findId(request);
        return ResponseEntity.ok(ApiResponse.success("아이디 찾기 성공", response));
    }

    /**
     * 비밀번호 찾기 (레거시 - Firebase SDK 사용 권장)
     * POST /api/auth/forgot-password
     *
     * ⚠️ 주의: Firebase를 사용하는 경우, Firebase SDK의 비밀번호 재설정 기능을 사용하세요.
     * 클라이언트에서 auth.sendPasswordResetEmail(email)을 호출하면 Firebase가 자동으로 재설정 메일을 발송합니다.
     *
     * 이 엔드포인트는 레거시 회원가입(/api/auth/signup)을 사용한 경우에만 필요합니다.
     */
    @Operation(
        summary = "비밀번호 찾기 (레거시)",
        description = """
            ⚠️ **Firebase SDK 사용을 권장합니다**

            **권장 방법 (Firebase):**
            ```javascript
            // React Native
            await auth().sendPasswordResetEmail(email);

            // React Web
            import { sendPasswordResetEmail } from 'firebase/auth';
            await sendPasswordResetEmail(auth, email);
            ```

            Firebase가 자동으로 비밀번호 재설정 이메일을 발송하고 처리합니다.

            **레거시 방법 (이 API):**
            레거시 회원가입을 사용한 경우, 비밀번호 재설정 링크를 이메일로 발송합니다.
            """
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        passwordResetService.createAndSendResetToken(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("비밀번호 재설정 메일이 발송되었습니다", null));
    }

    /**
     * 비밀번호 재설정 (레거시 - Firebase SDK 사용 권장)
     * POST /api/auth/reset-password
     *
     * ⚠️ 주의: Firebase를 사용하는 경우, Firebase SDK가 자동으로 처리합니다.
     *
     * 이 엔드포인트는 레거시 회원가입(/api/auth/signup)을 사용한 경우에만 필요합니다.
     */
    @Operation(
        summary = "비밀번호 재설정 (레거시)",
        description = """
            ⚠️ **Firebase SDK 사용을 권장합니다**

            **권장 방법 (Firebase):**
            Firebase가 이메일로 전송한 비밀번호 재설정 링크를 클릭하면,
            Firebase 호스팅 페이지에서 새 비밀번호를 입력하고 자동으로 재설정됩니다.
            서버 API 호출 없이 Firebase가 모든 것을 처리합니다.

            **레거시 방법 (이 API):**
            레거시 회원가입을 사용한 경우, 이메일로 받은 토큰으로 새로운 비밀번호를 설정합니다.
            """
    )
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
