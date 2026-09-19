package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {

    List<Course> findByClassName(String className);

    List<Course> findByLecturerId(String lecturerId);
}
