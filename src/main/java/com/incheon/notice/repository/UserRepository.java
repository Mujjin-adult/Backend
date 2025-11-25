package com.incheon.notice.repository;

import com.incheon.notice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 Repository
 * JpaRepository를 상속받으면 기본 CRUD 메서드가 자동 제공됨
 * (save, findById, findAll, delete 등)
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * 학번으로 사용자 조회
     */
    Optional<User> findByStudentId(String studentId);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 학번 존재 여부 확인
     */
    boolean existsByStudentId(String studentId);

    /**
     * FCM 토큰으로 사용자 조회
     */
    Optional<User> findByFcmToken(String fcmToken);

    /**
     * 이름과 학번으로 사용자 조회 (아이디 찾기용)
     */
    Optional<User> findByNameAndStudentId(String name, String studentId);

    /**
     * Firebase UID로 사용자 조회
     */
    Optional<User> findByFirebaseUid(String firebaseUid);

    /**
     * Firebase UID 존재 여부 확인
     */
    boolean existsByFirebaseUid(String firebaseUid);
}
