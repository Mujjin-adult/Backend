package com.incheon.notice.repository;

import com.incheon.notice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 카테고리 Repository
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 카테고리 코드로 조회
     */
    Optional<Category> findByCode(String code);

    /**
     * 활성 상태인 카테고리 목록 조회
     */
    List<Category> findByIsActiveTrue();

    /**
     * 카테고리 코드 존재 여부 확인
     */
    boolean existsByCode(String code);
}
