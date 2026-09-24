package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {

    List<Enrollment> findByCourseId(String courseId);

    List<Enrollment> findByStudentId(String studentId);

    Optional<Enrollment> findByCourseIdAndStudentId(String courseId, String studentId);

    boolean existsByCourseIdAndStudentId(String courseId, String studentId);

    long countByCourseId(String courseId);

    long countByCourseIdIn(List<String> courseIds);
}
