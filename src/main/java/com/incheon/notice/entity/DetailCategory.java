package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 상세 카테고리 엔티티
 * crawl_notice.category 값들을 저장하는 테이블
 * 사용자가 구독할 수 있는 알림 카테고리
 */
@Entity
@Table(name = "detail_categories", indexes = {
    @Index(name = "idx_detail_categories_name", columnList = "name")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DetailCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;  // 카테고리명 (예: "학사", "장학", "취업", "국가근로장학금" 등)
}
