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
@Tag(name = "인증 및 회원관리", description = "회원가입, 로그인, 아이디 찾기 API (Firebase Authentication 기반)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
     * Firebase 이메일 인증 메일 발송
     * POST /api/auth/send-verification-email
     *
     * ⚠️ 권장: 클라이언트에서 Firebase SDK의 sendEmailVerification()을 사용하는 것이 더 간단합니다.
     *
     * 이 API는 서버에서 커스텀 이메일 템플릿을 사용하거나 이메일 발송을 완전히 제어해야 하는 경우에 사용하세요.
     */
    @Operation(
        summary = "이메일 인증 메일 발송 (Firebase)",
        description = """
            Firebase 이메일 인증 링크를 생성하여 발송합니다.

            **⚠️ 권장 방법 (클라이언트):**
            ```javascript
            // React Native
            await user.sendEmailVerification();

            // React Web
            import { sendEmailVerification } from 'firebase/auth';
            await sendEmailVerification(user);
            ```

            **이 API 사용 시:**
            - 서버에서 커스텀 이메일 템플릿 사용 가능
            - 이메일 발송을 서버에서 완전히 제어

            Firebase 회원가입 후 이메일이 인증되지 않은 사용자에게 인증 메일을 발송합니다.
            """
    )
    @PostMapping("/send-verification-email")
    public ResponseEntity<ApiResponse<String>> sendVerificationEmail(@RequestParam String email) {
        String message = authService.sendEmailVerification(email);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    /**
     * Firebase 이메일 인증 메일 재발송
     * POST /api/auth/resend-verification-email
     */
    @Operation(
        summary = "이메일 인증 메일 재발송 (Firebase)",
        description = """
            Firebase 이메일 인증 메일을 재발송합니다.

            **⚠️ 권장 방법 (클라이언트):**
            ```javascript
            const user = auth().currentUser;
            await user.sendEmailVerification();
            ```

            이미 발송된 인증 메일을 받지 못한 경우 재발송합니다.
            """
    )
    @PostMapping("/resend-verification-email")
    public ResponseEntity<ApiResponse<String>> resendVerificationEmail(@RequestParam String email) {
        String message = authService.resendEmailVerification(email);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

}
