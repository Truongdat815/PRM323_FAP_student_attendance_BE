package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByMemberCode(String memberCode);

    Optional<User> findByFapCode(String fapCode);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByFapCode(String fapCode);
}
