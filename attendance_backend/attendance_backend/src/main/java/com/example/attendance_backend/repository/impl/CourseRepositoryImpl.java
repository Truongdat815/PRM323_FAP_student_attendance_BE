package com.example.attendance_backend.repository.impl;

import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.service.GoogleSheetsService;
import com.example.attendance_backend.util.SheetConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lớp thực thi CourseRepository sử dụng GoogleSheetsService tương tác với sheet COURSES.
 */
@Repository
public class CourseRepositoryImpl implements CourseRepository {

    private final GoogleSheetsService googleSheetsService;

    @Autowired
    public CourseRepositoryImpl(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @Override
    public Course save(Course course) throws IOException {
        googleSheetsService.appendRow(SheetConstants.SHEET_COURSES, course.toRow());
        return course;
    }

    @Override
    public Optional<Course> findById(String courseId) throws IOException {
        if (courseId == null || courseId.trim().isEmpty()) {
            return Optional.empty();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_COURSES);
        for (List<Object> row : rows) {
            if (!row.isEmpty() && courseId.equalsIgnoreCase(row.get(0).toString().trim())) {
                return Optional.ofNullable(Course.fromRow(row));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Course> findAll() throws IOException {
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_COURSES);
        List<Course> courses = new ArrayList<>();
        for (List<Object> row : rows) {
            Course course = Course.fromRow(row);
            if (course != null && course.getCourseId() != null && !course.getCourseId().isEmpty()) {
                courses.add(course);
            }
        }
        return courses;
    }

    @Override
    public List<Course> findByLecturerId(String lecturerId) throws IOException {
        if (lecturerId == null || lecturerId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_COURSES);
        List<Course> courses = new ArrayList<>();
        for (List<Object> row : rows) {
            if (row.size() > 4 && lecturerId.equalsIgnoreCase(row.get(4).toString().trim())) {
                Course course = Course.fromRow(row);
                if (course != null) {
                    courses.add(course);
                }
            }
        }
        return courses;
    }

    @Override
    public boolean update(String courseId, Course course) throws IOException {
        int rowIndex = googleSheetsService.findRowIndex(
                googleSheetsService.getDefaultSpreadsheetId(),
                SheetConstants.SHEET_COURSES,
                0,
                courseId
        );
        if (rowIndex == -1) {
            return false;
        }

        String range = SheetConstants.SHEET_COURSES + "!A" + rowIndex + ":F" + rowIndex;
        googleSheetsService.updateRow(range, course.toRow());
        return true;
    }

    @Override
    public boolean delete(String courseId) throws IOException {
        int rowIndex = googleSheetsService.findRowIndex(
                googleSheetsService.getDefaultSpreadsheetId(),
                SheetConstants.SHEET_COURSES,
                0,
                courseId
        );
        if (rowIndex == -1) {
            return false;
        }

        googleSheetsService.deleteRow(SheetConstants.SHEET_COURSES, rowIndex);
        return true;
    }
}
