package com.incheon.notice.service;

import com.incheon.notice.dto.AuthDto;
import com.incheon.notice.entity.User;
import com.incheon.notice.entity.UserRole;
import com.incheon.notice.repository.UserRepository;
import com.incheon.notice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스
 * 회원가입, 로그인, 토큰 갱신 처리
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    @Transactional
    public AuthDto.UserResponse signUp(AuthDto.SignUpRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 사용중인 이메일입니다");
        }

        // 학번 중복 체크
        if (userRepository.existsByStudentId(request.getStudentId())) {
            throw new RuntimeException("이미 사용중인 학번입니다");
        }

        // 사용자 생성
        User user = User.builder()
                .studentId(request.getStudentId())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(UserRole.USER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        return AuthDto.UserResponse.builder()
                .id(savedUser.getId())
                .studentId(savedUser.getStudentId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(savedUser.getRole().name())
                .build();
    }

    /**
     * 로그인
     */
    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        // 인증 처리
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(request.getEmail());

        // FCM 토큰 업데이트 (있는 경우)
        if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            user.updateFcmToken(request.getFcmToken());
        }

        // 사용자 정보 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        return AuthDto.LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationInSeconds())
                .user(AuthDto.UserResponse.builder()
                        .id(user.getId())
                        .studentId(user.getStudentId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    /**
     * 리프레시 토큰으로 액세스 토큰 갱신
     */
    public String refreshAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        return jwtTokenProvider.generateRefreshToken(email);
    }
}
