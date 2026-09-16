package com.example.attendance_backend.repository.impl;

import com.example.attendance_backend.model.Enrollment;
import com.example.attendance_backend.repository.EnrollmentRepository;
import com.example.attendance_backend.service.GoogleSheetsService;
import com.example.attendance_backend.util.SheetConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lớp thực thi EnrollmentRepository tương tác với sheet ENROLLMENTS qua GoogleSheetsService.
 */
@Repository
public class EnrollmentRepositoryImpl implements EnrollmentRepository {

    private final GoogleSheetsService googleSheetsService;

    @Autowired
    public EnrollmentRepositoryImpl(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @Override
    public Enrollment save(Enrollment enrollment) throws IOException {
        googleSheetsService.appendRow(SheetConstants.SHEET_ENROLLMENTS, enrollment.toRow());
        return enrollment;
    }

    @Override
    public Optional<Enrollment> findById(String enrollmentId) throws IOException {
        if (enrollmentId == null || enrollmentId.trim().isEmpty()) {
            return Optional.empty();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ENROLLMENTS);
        for (List<Object> row : rows) {
            if (!row.isEmpty() && enrollmentId.equalsIgnoreCase(row.get(0).toString().trim())) {
                return Optional.ofNullable(Enrollment.fromRow(row));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Enrollment> findByCourseId(String courseId) throws IOException {
        if (courseId == null || courseId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ENROLLMENTS);
        List<Enrollment> enrollments = new ArrayList<>();
        for (List<Object> row : rows) {
            if (row.size() > 1 && courseId.equalsIgnoreCase(row.get(1).toString().trim())) {
                Enrollment enrollment = Enrollment.fromRow(row);
                if (enrollment != null) {
                    enrollments.add(enrollment);
                }
            }
        }
        return enrollments;
    }

    @Override
    public List<Enrollment> findByStudentId(String studentId) throws IOException {
        if (studentId == null || studentId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ENROLLMENTS);
        List<Enrollment> enrollments = new ArrayList<>();
        for (List<Object> row : rows) {
            if (row.size() > 2 && studentId.equalsIgnoreCase(row.get(2).toString().trim())) {
                Enrollment enrollment = Enrollment.fromRow(row);
                if (enrollment != null) {
                    enrollments.add(enrollment);
                }
            }
        }
        return enrollments;
    }

    @Override
    public boolean existsByCourseIdAndStudentId(String courseId, String studentId) throws IOException {
        if (courseId == null || studentId == null) {
            return false;
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ENROLLMENTS);
        for (List<Object> row : rows) {
            if (row.size() > 2) {
                String cId = row.get(1).toString().trim();
                String sId = row.get(2).toString().trim();
                if (courseId.equalsIgnoreCase(cId) && studentId.equalsIgnoreCase(sId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Enrollment> findAll() throws IOException {
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ENROLLMENTS);
        List<Enrollment> enrollments = new ArrayList<>();
        for (List<Object> row : rows) {
            Enrollment enrollment = Enrollment.fromRow(row);
            if (enrollment != null && enrollment.getEnrollmentId() != null && !enrollment.getEnrollmentId().isEmpty()) {
                enrollments.add(enrollment);
            }
        }
        return enrollments;
    }
}
