package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 카테고리 엔티티
 * 학과, 대학, 행정부서 등의 공지사항 카테고리
 * 예: 컴퓨터공학과, 공과대학, 학사공지 등
 */
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_code", columnList = "code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;  // 카테고리 코드 (예: CS, ENG, ACADEMIC)

    @Column(nullable = false, length = 100)
    private String name;  // 카테고리 이름 (예: 컴퓨터공학과, 공과대학)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type;  // 카테고리 유형

    @Column(length = 255)
    private String url;  // 크롤링할 웹사이트 URL

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;  // 활성 상태

    @Column(length = 500)
    private String description;  // 설명

    /**
     * 카테고리 정보 수정
     */
    public void updateInfo(String name, String url, String description) {
        this.name = name;
        this.url = url;
        this.description = description;
    }

    /**
     * 활성 상태 변경
     */
    public void updateActiveStatus(Boolean isActive) {
        this.isActive = isActive;
    }
}

