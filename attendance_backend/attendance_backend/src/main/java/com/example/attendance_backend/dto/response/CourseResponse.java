package com.example.attendance_backend.dto.response;

import com.example.attendance_backend.model.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO trả về thông tin chi tiết khóa học cho Client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private String courseId;
    private String subjectCode;
    private String className;
    private String semester;
    private String lecturerId;
    private String lecturerName;
    private LocalDateTime createdAt;

    public static CourseResponse fromCourse(Course course) {
        return fromCourse(course, null);
    }

    public static CourseResponse fromCourse(Course course, String lecturerName) {
        if (course == null) {
            return null;
        }
        return CourseResponse.builder()
                .courseId(course.getCourseId())
                .subjectCode(course.getSubjectCode())
                .className(course.getClassName())
                .semester(course.getSemester())
                .lecturerId(course.getLecturerId())
                .lecturerName(lecturerName)
                .createdAt(course.getCreatedAt())
                .build();
    }
}
