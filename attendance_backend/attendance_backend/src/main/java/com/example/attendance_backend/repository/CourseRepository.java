package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.Course;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface cho các thao tác dữ liệu khóa học trên sheet COURSES.
 */
public interface CourseRepository {

    Course save(Course course) throws IOException;

    Optional<Course> findById(String courseId) throws IOException;

    List<Course> findAll() throws IOException;

    List<Course> findByLecturerId(String lecturerId) throws IOException;

    boolean update(String courseId, Course course) throws IOException;

    boolean delete(String courseId) throws IOException;
}
