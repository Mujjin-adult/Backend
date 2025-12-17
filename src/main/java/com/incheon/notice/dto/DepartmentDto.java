package com.incheon.notice.dto;

import com.incheon.notice.entity.Department;
import lombok.*;

/**
 * 학과 관련 DTO
 */
public class DepartmentDto {

    /**
     * 학과 정보 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long id;
        private String name;      // 학과명
        private String college;   // 소속 대학

        /**
         * Entity -> DTO 변환
         */
        public static Response from(Department department) {
            if (department == null) {
                return null;
            }
            return Response.builder()
                    .id(department.getId())
                    .name(department.getName())
                    .college(department.getCollege())
                    .build();
        }
    }
}
