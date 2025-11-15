package com.incheon.notice.security;

import com.incheon.notice.entity.User;
import com.incheon.notice.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Spring Security 사용자 인증을 위한 UserDetailsService 구현
 * 사용자 정보를 데이터베이스에서 조회
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                getAuthorities(user),
                user.getIsActive()
        );
    }

    /**
     * 사용자 권한 목록 생성
     */
    private List<GrantedAuthority> getAuthorities(User user) {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    /**
     * userId를 포함한 Custom UserDetails 구현
     */
    @Getter
    public static class CustomUserDetails implements UserDetails {

        private final Long userId;
        private final String email;
        private final String password;
        private final Collection<? extends GrantedAuthority> authorities;
        private final boolean isActive;

        public CustomUserDetails(Long userId, String email, String password,
                                Collection<? extends GrantedAuthority> authorities, boolean isActive) {
            this.userId = userId;
            this.email = email;
            this.password = password;
            this.authorities = authorities;
            this.isActive = isActive;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return email;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return isActive;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return isActive;
        }
    }
}
