package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {

    List<Course> findByClassName(String className);

    List<Course> findByLecturerId(String lecturerId);

    @Query("SELECT c FROM Course c WHERE " +
           "(:className IS NULL OR :className = '' OR LOWER(c.className) LIKE LOWER(CONCAT('%', :className, '%'))) AND " +
           "(:subjectCode IS NULL OR :subjectCode = '' OR LOWER(c.subjectCode) LIKE LOWER(CONCAT('%', :subjectCode, '%'))) AND " +
           "(:semester IS NULL OR :semester = '' OR LOWER(c.semester) LIKE LOWER(CONCAT('%', :semester, '%'))) AND " +
           "(:lecturerId IS NULL OR :lecturerId = '' OR c.lecturerId = :lecturerId) AND " +
           "(:lecturerName IS NULL OR :lecturerName = '' OR EXISTS (" +
           "    SELECT u FROM User u WHERE u.userId = c.lecturerId AND (" +
           "        LOWER(u.fullName) LIKE LOWER(CONCAT('%', :lecturerName, '%')) OR " +
           "        LOWER(u.username) LIKE LOWER(CONCAT('%', :lecturerName, '%'))" +
           "    )" +
           "))")
    List<Course> searchCourses(
            @Param("className") String className,
            @Param("subjectCode") String subjectCode,
            @Param("semester") String semester,
            @Param("lecturerName") String lecturerName,
            @Param("lecturerId") String lecturerId);

    default List<Course> searchCourses(String className, String subjectCode, String semester, String lecturerName) {
        return searchCourses(className, subjectCode, semester, lecturerName, null);
    }
}
