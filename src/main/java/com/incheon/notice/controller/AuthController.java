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
     * 회원가입 (Firebase 통합)
     * POST /api/auth/signup
     *
     * 서버에서 Firebase Authentication에 사용자를 생성하고 DB에 저장합니다.
     * 회원가입 후 반드시 클라이언트에서 로그인하여 idToken과 fcmToken을 등록해야 합니다.
     */
    @Operation(
        summary = "회원가입 (Firebase 통합)",
        description = """
            서버에서 Firebase Authentication에 사용자를 생성하고 DB에 저장합니다.

            **플로우:**
            1. **회원가입 API 호출** (이 엔드포인트)
               - 서버: Firebase에 사용자 생성 + DB 저장
               - 서버: 이메일 인증 링크 발송

            2. **클라이언트: Firebase 로그인**
               ```javascript
               // React Native 예시
               import auth from '@react-native-firebase/auth';

               const userCredential = await auth().signInWithEmailAndPassword(email, password);
               const idToken = await userCredential.user.getIdToken();
               ```

            3. **클라이언트: FCM 토큰 발급**
               ```javascript
               import messaging from '@react-native-firebase/messaging';

               const fcmToken = await messaging().getToken();
               ```

            4. **로그인 API 호출** (`POST /api/auth/login`)
               ```json
               {
                 "idToken": "eyJhbGc...",
                 "fcmToken": "dW4f2..."
               }
               ```

            **중요:**
            - ⚠️ idToken과 fcmToken은 서버에서 발급할 수 없습니다
            - ⚠️ 회원가입 후 반드시 위 2-4 단계를 진행해야 합니다
            - 이메일 인증은 선택사항 (인증 전에도 로그인 가능)

            **대안 방법 (클라이언트 우선):**
            1. 클라이언트: Firebase SDK로 직접 회원가입 `createUserWithEmailAndPassword()`
            2. 클라이언트: ID Token 발급
            3. 서버: `/api/auth/login` 호출 시 자동으로 DB에 사용자 생성
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
     * 이메일/비밀번호 로그인 (간편 로그인)
     * POST /api/auth/login/email
     *
     * 서버에서 이메일/비밀번호를 검증하고 Firebase 커스텀 토큰을 발급합니다.
     * 가장 간단한 로그인 방법입니다.
     */
    @Operation(
        summary = "이메일/비밀번호 로그인 (간편)",
        description = """
            이메일과 비밀번호로 간편하게 로그인합니다.

            **사용법:**
            ```bash
            POST /api/auth/login/email
            {
              "email": "test@inu.ac.kr",
              "password": "password123",
              "fcmToken": "dW4f2..." (선택사항)
            }
            ```

            **응답:**
            ```json
            {
              "success": true,
              "data": {
                "idToken": "eyJhbGc...",  // Firebase 커스텀 토큰
                "tokenType": "Bearer",
                "expiresIn": 3600,
                "user": {
                  "id": 1,
                  "email": "test@inu.ac.kr",
                  "name": "홍길동"
                }
              }
            }
            ```

            **주의:**
            - ✅ 회원가입 직후 바로 사용 가능
            - ✅ Firebase SDK 없이도 로그인 가능
            - ⚠️ idToken(커스텀 토큰)은 Firebase 로그인 시에만 사용
            - 💡 API 인증에는 이 토큰을 그대로 사용하세요

            **클라이언트 사용 예시:**
            ```javascript
            const response = await fetch('/api/auth/login/email', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                email: 'test@inu.ac.kr',
                password: 'password123'
              })
            });

            const { idToken, user } = await response.json();

            // API 요청 시 토큰 사용
            fetch('/api/notices', {
              headers: { 'Authorization': `Bearer ${idToken}` }
            });
            ```
            """
    )
    @PostMapping("/login/email")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> loginWithEmail(
            @Valid @RequestBody AuthDto.EmailLoginRequest request) {
        AuthDto.LoginResponse response = authService.loginWithEmail(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
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
     * Firebase UID로 로그인 (테스트/개발용)
     * POST /api/auth/login/uid
     *
     * Firebase 콘솔에서 확인한 UID를 직접 사용하여 로그인합니다.
     */
    @Operation(
        summary = "Firebase UID 로그인 (테스트용)",
        description = """
            Firebase UID를 직접 사용하여 로그인합니다.

            **사용 방법:**
            1. Firebase 콘솔 > Authentication > Users에서 UID 확인
            2. 이 API에 UID 전송

            **요청 예시:**
            ```json
            {
              "firebaseUid": "abc123xyz...",
              "fcmToken": "dW4f2..." (선택사항)
            }
            ```

            **응답:**
            ```json
            {
              "success": true,
              "data": {
                "idToken": "eyJhbGc...",  // Firebase 커스텀 토큰
                "tokenType": "Bearer",
                "expiresIn": 3600,
                "user": { ... }
              }
            }
            ```

            **주의:**
            - ⚠️ 테스트/개발 목적으로만 사용하세요
            - ⚠️ 프로덕션에서는 `/api/auth/login` 또는 `/api/auth/login/email` 사용 권장
            - 사용자가 DB에 등록되어 있어야 합니다 (회원가입 필요)
            """
    )
    @PostMapping("/login/uid")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> loginWithFirebaseUid(
            @Valid @RequestBody AuthDto.FirebaseUidLoginRequest request) {
        AuthDto.LoginResponse response = authService.loginWithFirebaseUid(request);
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

    /**
     * 비밀번호 재설정 요청 (Firebase)
     * POST /api/auth/forgot-password
     *
     * Firebase를 사용하여 비밀번호 재설정 이메일을 발송합니다.
     */
    @Operation(
        summary = "비밀번호 재설정 요청",
        description = """
            비밀번호를 잊어버린 사용자에게 재설정 이메일을 발송합니다.

            **Firebase 기반:**
            - Firebase Admin SDK로 비밀번호 재설정 링크 생성
            - 이메일로 재설정 링크 발송
            - 사용자는 링크를 클릭하여 새 비밀번호 설정

            **사용 방법:**
            1. 이메일 입력하여 요청
            2. 이메일로 재설정 링크 수신
            3. 링크 클릭하여 새 비밀번호 입력
            4. Firebase에서 자동으로 비밀번호 업데이트

            **제한:**
            - 동일 이메일로 1시간에 최대 3회까지 요청 가능
            """
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        String message = authService.sendPasswordResetEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

}
