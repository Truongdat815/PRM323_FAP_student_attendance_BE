package com.example.attendance_backend.security;

import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Triển khai UserDetailsService lấy thông tin tài khoản từ sheet USERS.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
            return new CustomUserDetails(user);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi đọc dữ liệu từ Google Sheets: " + e.getMessage(), e);
        }
    }
}
