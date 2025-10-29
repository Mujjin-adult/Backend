package com.incheon.notice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티의 기본 클래스
 * 생성일시, 수정일시를 자동으로 관리
 */
@Getter
@MappedSuperclass  // 이 클래스를 상속받는 엔티티들에게 필드를 제공
@EntityListeners(AuditingEntityListener.class)  // 자동으로 시간 관리
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
