package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.CourseRequest;
import com.example.attendance_backend.dto.response.CourseResponse;

import java.io.IOException;
import java.util.List;

/**
 * Interface nghiệp vụ quản lý khóa học (Courses Management).
 */
public interface CourseService {

    List<CourseResponse> getAllCourses(String lecturerId) throws IOException;

    CourseResponse getCourseById(String courseId) throws IOException;

    CourseResponse createCourse(CourseRequest request, String currentUsername) throws IOException;

    CourseResponse updateCourse(String courseId, CourseRequest request, String currentUsername) throws IOException;

    void deleteCourse(String courseId, String currentUsername) throws IOException;
}
