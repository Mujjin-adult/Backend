package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 학과 엔티티
 * 사용자의 소속 학과 정보를 저장
 */
@Entity
@Table(name = "departments", indexes = {
    @Index(name = "idx_departments_name", columnList = "name"),
    @Index(name = "idx_departments_college", columnList = "college")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;  // 학과명 (예: 컴퓨터공학과)

    @Column(length = 100)
    private String college;  // 소속 대학 (예: 공과대학)
}
