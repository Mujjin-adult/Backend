package com.incheon.notice.repository;

import com.incheon.notice.entity.DetailCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 상세 카테고리 Repository
 */
@Repository
public interface DetailCategoryRepository extends JpaRepository<DetailCategory, Long> {

    /**
     * 카테고리 이름으로 조회
     */
    Optional<DetailCategory> findByName(String name);

    /**
     * 카테고리 이름 존재 여부 확인
     */
    boolean existsByName(String name);

    /**
     * 활성화된 카테고리 목록 조회
     */
    List<DetailCategory> findByIsActiveTrueOrderByNameAsc();

    /**
     * 모든 카테고리 이름순 조회
     */
    List<DetailCategory> findAllByOrderByNameAsc();

    /**
     * crawl_notice 테이블에서 고유한 category 값 추출
     */
    @Query(value = "SELECT DISTINCT cn.category FROM crawl_notice cn WHERE cn.category IS NOT NULL AND cn.category != ''", nativeQuery = true)
    List<String> findDistinctCategoriesFromCrawlNotice();
}
