package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 엔티티
 * 데이터베이스의 users 테이블과 매핑
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_student_id", columnList = "student_id"),
    @Index(name = "idx_firebase_uid", columnList = "firebase_uid")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String studentId;  // 학번 (nullable - 나중에 입력 가능)

    @Column(unique = true, length = 50)
    private String firebaseUid;  // Firebase Authentication UID (nullable - 레거시 회원가입 시 null)

    @Column(nullable = false, unique = true, length = 100)
    private String email;  // 이메일

    @Column(nullable = false)
    private String password;  // 암호화된 비밀번호

    @Column(length = 50)
    private String name;  // 이름

    @Column(length = 255)
    private String fcmToken;  // FCM 푸시 알림 토큰

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;  // 사용자 권한

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;  // 활성 상태

    @Column(length = 100)
    private String department;  // 학과

    @Column(nullable = false)
    @Builder.Default
    private Boolean systemNotificationEnabled = true;  // 시스템 알림 허용/미허용

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;  // 이메일 인증 여부

    // 사용자가 저장한 북마크 목록 (양방향 관계)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Bookmark> bookmarks = new ArrayList<>();

    // 사용자의 알림 설정 (양방향 관계)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserPreference> preferences = new ArrayList<>();

    /**
     * FCM 토큰 업데이트
     */
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * 비밀번호 변경
     */
    public void updatePassword(String password) {
        this.password = password;
    }

    /**
     * 사용자 정보 수정
     */
    public void updateInfo(String name, String email) {
        this.name = name;
        this.email = email;
    }

    /**
     * 학과 설정
     */
    public void updateDepartment(String department) {
        this.department = department;
    }

    /**
     * 시스템 알림 설정 변경
     */
    public void updateSystemNotification(Boolean enabled) {
        this.systemNotificationEnabled = enabled;
    }

    /**
     * 이름 수정
     */
    public void updateName(String name) {
        this.name = name;
    }

    /**
     * 계정 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 이메일 인증 완료 처리
     */
    public void verifyEmail() {
        this.isEmailVerified = true;
    }

    /**
     * 학번 설정 (Firebase 로그인 후 학번 입력 시)
     */
    public void updateStudentId(String studentId) {
        this.studentId = studentId;
    }

    /**
     * Firebase UID 설정
     */
    public void updateFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }
}

