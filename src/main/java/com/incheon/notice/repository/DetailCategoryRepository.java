package com.incheon.notice.repository;

import com.incheon.notice.entity.DetailCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 상세 카테고리 Repository
 */
@Repository
public interface DetailCategoryRepository extends JpaRepository<DetailCategory, Long> {

    /**
     * 전체 상세 카테고리 목록 조회 (이름순 정렬)
     */
    List<DetailCategory> findAllByOrderByNameAsc();

    /**
     * 카테고리명으로 조회
     */
    Optional<DetailCategory> findByName(String name);

    /**
     * 카테고리명 존재 여부 확인
     */
    boolean existsByName(String name);
}
