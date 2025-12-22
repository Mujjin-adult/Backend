package com.incheon.notice.security;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 인증 필터
 * 요청의 Authorization 헤더에서 토큰을 추출하고 검증
 *
 * 지원하는 토큰:
 * 1. 서버 JWT 토큰 (이메일 로그인으로 발급)
 * 2. Firebase ID Token (Firebase SDK 로그인으로 발급)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final FirebaseTokenProvider firebaseTokenProvider;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Authorization 헤더에서 토큰 추출
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // 1. 먼저 서버 JWT 토큰인지 확인
                if (jwtTokenProvider.validateToken(token)) {
                    authenticateWithServerJwt(token, request);
                } else {
                    // 2. Firebase ID Token 검증 시도
                    authenticateWithFirebase(token, request);
                }
            }
        } catch (FirebaseAuthException e) {
            log.debug("Failed to verify Firebase token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.debug("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 서버 JWT 토큰으로 인증
     */
    private void authenticateWithServerJwt(String token, HttpServletRequest request) {
        String email = jwtTokenProvider.getEmailFromToken(token);

        // 사용자 정보 로드
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Spring Security 인증 객체 생성
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // SecurityContext에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Server JWT authentication set for user: {}", email);
    }

    /**
     * Firebase ID Token으로 인증
     */
    private void authenticateWithFirebase(String token, HttpServletRequest request) throws FirebaseAuthException {
        FirebaseToken firebaseToken = firebaseTokenProvider.verifyToken(token);
        String email = firebaseToken.getEmail();

        // 사용자 정보 로드
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Spring Security 인증 객체 생성
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // SecurityContext에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Firebase authentication set for user: {}", email);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
