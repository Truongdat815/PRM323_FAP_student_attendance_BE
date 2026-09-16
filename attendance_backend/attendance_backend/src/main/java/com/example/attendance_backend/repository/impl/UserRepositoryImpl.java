package com.example.attendance_backend.repository.impl;

import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.GoogleSheetsService;
import com.example.attendance_backend.util.SheetConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lớp thực thi UserRepository sử dụng GoogleSheetsService để đọc/ghi từ sheet USERS.
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final GoogleSheetsService googleSheetsService;

    @Autowired
    public UserRepositoryImpl(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @Override
    public User save(User user) throws IOException {
        googleSheetsService.appendRow(SheetConstants.SHEET_USERS, user.toRow());
        return user;
    }

    @Override
    public Optional<User> findById(String userId) throws IOException {
        if (userId == null || userId.trim().isEmpty()) {
            return Optional.empty();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_USERS);
        for (List<Object> row : rows) {
            if (!row.isEmpty() && userId.equalsIgnoreCase(row.get(0).toString().trim())) {
                return Optional.ofNullable(User.fromRow(row));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) throws IOException {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_USERS);
        for (List<Object> row : rows) {
            if (row.size() > 1 && username.equalsIgnoreCase(row.get(1).toString().trim())) {
                return Optional.ofNullable(User.fromRow(row));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByFapCode(String fapCode) throws IOException {
        if (fapCode == null || fapCode.trim().isEmpty()) {
            return Optional.empty();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_USERS);
        for (List<Object> row : rows) {
            if (row.size() > 3 && fapCode.equalsIgnoreCase(row.get(3).toString().trim())) {
                return Optional.ofNullable(User.fromRow(row));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByUsername(String username) throws IOException {
        return findByUsername(username).isPresent();
    }

    @Override
    public boolean existsByFapCode(String fapCode) throws IOException {
        return findByFapCode(fapCode).isPresent();
    }

    @Override
    public List<User> findAll() throws IOException {
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_USERS);
        List<User> users = new ArrayList<>();
        for (List<Object> row : rows) {
            User user = User.fromRow(row);
            if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
                users.add(user);
            }
        }
        return users;
    }
}
