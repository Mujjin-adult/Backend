package com.incheon.notice.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Firebase ID Token 검증 유틸리티
 * JWT 대신 Firebase Authentication 사용
 */
@Slf4j
@Component
public class FirebaseTokenProvider {

    /**
     * Firebase ID Token 검증
     *
     * @param idToken Firebase에서 발급한 ID Token
     * @return FirebaseToken 객체 (사용자 정보 포함)
     * @throws FirebaseAuthException 토큰이 유효하지 않을 경우
     */
    public FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
        try {
            // Firebase Admin SDK를 사용하여 ID Token 검증
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            log.debug("Firebase token verified successfully for user: {}", decodedToken.getUid());
            return decodedToken;
        } catch (FirebaseAuthException e) {
            log.error("Failed to verify Firebase token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 토큰에서 사용자 이메일 추출
     */
    public String getEmailFromToken(String idToken) {
        try {
            FirebaseToken decodedToken = verifyToken(idToken);
            return decodedToken.getEmail();
        } catch (FirebaseAuthException e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 사용자 UID 추출
     */
    public String getUidFromToken(String idToken) {
        try {
            FirebaseToken decodedToken = verifyToken(idToken);
            return decodedToken.getUid();
        } catch (FirebaseAuthException e) {
            log.error("Failed to extract UID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰 유효성 검증 (boolean 반환)
     */
    public boolean validateToken(String idToken) {
        try {
            verifyToken(idToken);
            return true;
        } catch (FirebaseAuthException e) {
            return false;
        }
    }
}
