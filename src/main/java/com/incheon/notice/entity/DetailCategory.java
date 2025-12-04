package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 상세 카테고리 엔티티 (detail_category 테이블)
 * crawl_notice의 category 값을 저장하여 사용자가 구독할 수 있는 상세 카테고리
 */
@Entity
@Table(name = "detail_category", indexes = {
    @Index(name = "idx_detail_category_name", columnList = "name")
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
    private String name;  // 카테고리 이름 (예: 학사, 장학금, 모집 등)

    @Column(length = 255)
    private String description;  // 카테고리 설명

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;  // 활성 상태

    /**
     * 카테고리 설명 업데이트
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 활성 상태 변경
     */
    public void setActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
