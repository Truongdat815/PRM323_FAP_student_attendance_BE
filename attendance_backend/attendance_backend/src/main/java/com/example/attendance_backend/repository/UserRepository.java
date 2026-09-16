package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface thao tác truy vấn dữ liệu người dùng trên sheet USERS.
 */
public interface UserRepository {

    User save(User user) throws IOException;

    Optional<User> findById(String userId) throws IOException;

    Optional<User> findByUsername(String username) throws IOException;

    Optional<User> findByFapCode(String fapCode) throws IOException;

    boolean existsByUsername(String username) throws IOException;

    boolean existsByFapCode(String fapCode) throws IOException;

    List<User> findAll() throws IOException;
}
