package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.Enrollment;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface thao tác với sheet ENROLLMENTS.
 */
public interface EnrollmentRepository {

    Enrollment save(Enrollment enrollment) throws IOException;

    Optional<Enrollment> findById(String enrollmentId) throws IOException;

    List<Enrollment> findByCourseId(String courseId) throws IOException;

    List<Enrollment> findByStudentId(String studentId) throws IOException;

    boolean existsByCourseIdAndStudentId(String courseId, String studentId) throws IOException;

    List<Enrollment> findAll() throws IOException;
}
