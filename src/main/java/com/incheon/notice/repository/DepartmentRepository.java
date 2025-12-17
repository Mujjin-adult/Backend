package com.incheon.notice.repository;

import com.incheon.notice.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 학과 Repository
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * 전체 학과 목록 조회 (대학, 학과명 순 정렬)
     */
    List<Department> findAllByOrderByCollegeAscNameAsc();

    /**
     * 특정 대학의 학과 목록 조회
     */
    List<Department> findByCollegeOrderByNameAsc(String college);

    /**
     * 학과명으로 조회
     */
    Optional<Department> findByName(String name);

    /**
     * 학과명 존재 여부 확인
     */
    boolean existsByName(String name);
}
