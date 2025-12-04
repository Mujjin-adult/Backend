package com.incheon.notice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.incheon.notice.dto.ApiResponse;
import com.incheon.notice.entity.User;
import com.incheon.notice.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

/**
 * Firebase Authentication 필터
 * 요청의 Authorization 헤더에서 Firebase ID Token을 추출하고 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final FirebaseTokenProvider firebaseTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Authorization 헤더에서 토큰 추출
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // Firebase ID Token 검증
                FirebaseToken firebaseToken = firebaseTokenProvider.verifyToken(token);
                String email = firebaseToken.getEmail();

                if (email == null) {
                    log.warn("Firebase token does not contain email");
                    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Firebase 토큰에 이메일 정보가 없습니다.");
                    return;
                }

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
        } catch (FirebaseAuthException e) {
            // Firebase ID Token 검증 실패 시 커스텀 토큰(테스트용) 처리 시도
            String token = getTokenFromRequest(request);
            if (token != null && tryAuthenticateWithCustomToken(token, request)) {
                log.debug("Authenticated with custom token (test mode)");
                filterChain.doFilter(request, response);
                return;
            }

            log.error("Firebase authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 Firebase 토큰입니다: " + e.getMessage());
            return;
        } catch (UsernameNotFoundException e) {
            log.error("User not found in database: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "등록되지 않은 사용자입니다. 먼저 회원가입을 해주세요.");
            return;
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 인증 오류 응답 전송
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> errorResponse = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
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

    /**
     * Firebase 커스텀 토큰으로 인증 시도 (테스트/개발용)
     * 커스텀 토큰에서 UID를 추출하여 DB에서 사용자를 찾아 인증
     */
    private boolean tryAuthenticateWithCustomToken(String token, HttpServletRequest request) {
        try {
            // JWT 형식의 커스텀 토큰에서 payload 추출
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            // payload 디코딩
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            // uid 추출 (간단한 파싱)
            String uid = null;
            if (payload.contains("\"uid\":\"")) {
                int start = payload.indexOf("\"uid\":\"") + 7;
                int end = payload.indexOf("\"", start);
                if (end > start) {
                    uid = payload.substring(start, end);
                }
            }

            if (uid == null || uid.isEmpty()) {
                log.debug("Could not extract UID from custom token");
                return false;
            }

            // Firebase UID로 사용자 조회
            Optional<User> userOpt = userRepository.findByFirebaseUid(uid);
            if (userOpt.isEmpty()) {
                log.debug("User not found with Firebase UID: {}", uid);
                return false;
            }

            User user = userOpt.get();

            // 사용자 정보 로드
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());

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

            log.info("Custom token authentication successful for user: {} (test mode)", user.getEmail());
            return true;

        } catch (Exception e) {
            log.debug("Custom token authentication failed: {}", e.getMessage());
            return false;
        }
    }
}
