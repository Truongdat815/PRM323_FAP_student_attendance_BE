package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.ChangePasswordRequest;
import com.example.attendance_backend.dto.request.UpdateProfileRequest;
import com.example.attendance_backend.dto.response.UserResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GoogleSheetsService googleSheetsService;

    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder, googleSheetsService);

        sampleUser = User.builder()
                .userId("USR-123")
                .username("old_username")
                .fullName("Nguyễn Văn A")
                .email("old_email@fpt.edu.vn")
                .passwordHash("hashed_password")
                .roleId("ROLE_STUDENT")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testUpdateProfile_AllFields_Success() throws IOException {
        when(userRepository.findByUsername("old_username")).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByUsername("new_username")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new_email@fpt.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .username("new_username")
                .fullName("Nguyễn Văn B")
                .email("new_email@fpt.edu.vn")
                .build();

        UserResponse response = userService.updateProfile(request, "old_username");

        assertNotNull(response);
        assertEquals("new_username", response.getUsername());
        assertEquals("Nguyễn Văn B", response.getFullName());
        assertEquals("new_email@fpt.edu.vn", response.getEmail());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void testUpdateProfile_OnlyUsername_Success() throws IOException {
        when(userRepository.findByUsername("old_username")).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByUsername("new_username")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .username("new_username")
                .build();

        UserResponse response = userService.updateProfile(request, "old_username");

        assertNotNull(response);
        assertEquals("new_username", response.getUsername());
        assertEquals("Nguyễn Văn A", response.getFullName()); // Unchanged
        assertEquals("old_email@fpt.edu.vn", response.getEmail()); // Unchanged
    }

    @Test
    void testUpdateProfile_DuplicateUsername_ThrowsConflict() {
        User existingUser = User.builder()
                .userId("USR-OTHER")
                .username("existing_username")
                .build();

        when(userRepository.findByUsername("old_username")).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByUsername("existing_username")).thenReturn(Optional.of(existingUser));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .username("existing_username")
                .build();

        AppException ex = assertThrows(AppException.class, () -> userService.updateProfile(request, "old_username"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Tên đăng nhập đã được sử dụng"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdateProfile_ShortUsername_ThrowsBadRequest() {
        when(userRepository.findByUsername("old_username")).thenReturn(Optional.of(sampleUser));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .username("ab") // < 3 characters
                .build();

        AppException ex = assertThrows(AppException.class, () -> userService.updateProfile(request, "old_username"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("từ 3 đến 50 ký tự"));
    }

    @Test
    void testUpdateProfile_DuplicateEmail_ThrowsBadRequest() {
        User existingUser = User.builder()
                .userId("USR-OTHER")
                .email("other@fpt.edu.vn")
                .build();

        when(userRepository.findByUsername("old_username")).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByEmail("other@fpt.edu.vn")).thenReturn(Optional.of(existingUser));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .email("other@fpt.edu.vn")
                .build();

        AppException ex = assertThrows(AppException.class, () -> userService.updateProfile(request, "old_username"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Email đã được sử dụng"));
    }

    @Test
    void testChangePassword_Success() throws IOException {
        when(userRepository.findByUsername("old_username")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldPass123", "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("newPass456")).thenReturn("new_hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPass123")
                .newPassword("newPass456")
                .build();

        userService.changePassword(request, "old_username");

        assertEquals("new_hashed_password", sampleUser.getPasswordHash());
        verify(userRepository, times(1)).save(sampleUser);
    }
}
