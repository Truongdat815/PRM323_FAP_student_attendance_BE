package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.LoginRequest;
import com.example.attendance_backend.dto.request.RegisterRequest;
import com.example.attendance_backend.dto.response.AuthResponse;
import com.example.attendance_backend.dto.response.UserResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.security.JwtTokenProvider;
import com.example.attendance_backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void testRegisterStudentSuccess() throws IOException {
        RegisterRequest request = RegisterRequest.builder()
                .username("student1")
                .password("password123")
                .email("student1@fpt.edu.vn")
                .fullName("Nguyen Van A")
                .roleId("STUDENT")
                .fapCode("SE192964")
                .build();

        when(userRepository.existsByUsername("student1")).thenReturn(false);
        when(userRepository.existsByFapCode("SE192964")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("student1", response.getUsername());
        assertEquals("SE192964", response.getFapCode());
        assertEquals("ROLE_STUDENT", response.getRoleId());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterLecturerSuccessWithoutFapCode() throws IOException {
        RegisterRequest request = RegisterRequest.builder()
                .username("lecturer1")
                .password("pass123")
                .email("lecturer1@fpt.edu.vn")
                .fullName("Tran Thi B")
                .roleId("LECTURER")
                .fapCode(null)
                .build();

        when(userRepository.existsByUsername("lecturer1")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("lecturer1", response.getUsername());
        assertEquals("", response.getFapCode());
        assertEquals("ROLE_LECTURER", response.getRoleId());
    }

    @Test
    void testRegisterStudentMissingFapCodeThrowsException() throws IOException {
        RegisterRequest request = RegisterRequest.builder()
                .username("student2")
                .password("pass123")
                .email("student2@fpt.edu.vn")
                .fullName("Le Van C")
                .roleId("STUDENT")
                .fapCode("")
                .build();

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("bắt buộc đối với tài khoản sinh viên"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterStudentInvalidFapCodeRegexThrowsException() throws IOException {
        RegisterRequest request = RegisterRequest.builder()
                .username("student3")
                .password("pass123")
                .email("student3@fpt.edu.vn")
                .fullName("Pham Van D")
                .roleId("STUDENT")
                .fapCode("123SE") // Sai định dạng
                .build();

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("không đúng định dạng"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterDuplicateUsernameThrowsException() throws IOException {
        RegisterRequest request = RegisterRequest.builder()
                .username("existingUser")
                .password("pass123")
                .email("test@fpt.edu.vn")
                .fullName("Test User")
                .roleId("STUDENT")
                .fapCode("SE123456")
                .build();

        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("đã được sử dụng"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterDuplicateFapCodeThrowsException() throws IOException {
        RegisterRequest request = RegisterRequest.builder()
                .username("newUser")
                .password("pass123")
                .email("test@fpt.edu.vn")
                .fullName("Test User")
                .roleId("STUDENT")
                .fapCode("SE123456")
                .build();

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByFapCode("SE123456")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("đã tồn tại"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testLoginSuccess() throws IOException {
        LoginRequest request = LoginRequest.builder()
                .username("student1")
                .password("password123")
                .build();

        User user = User.builder()
                .userId("USR-123")
                .username("student1")
                .passwordHash("hashedPassword")
                .roleId("ROLE_STUDENT")
                .fapCode("SE192964")
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(user)).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("student1", response.getUser().getUsername());
    }

    @Test
    void testLoginWrongPasswordThrowsException() throws IOException {
        LoginRequest request = LoginRequest.builder()
                .username("student1")
                .password("wrongpassword")
                .build();

        User user = User.builder()
                .userId("USR-123")
                .username("student1")
                .passwordHash("hashedPassword")
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Tên tài khoản hoặc mật khẩu không chính xác"));
    }

    @Test
    void testLoginUserNotFoundThrowsException() throws IOException {
        LoginRequest request = LoginRequest.builder()
                .username("nonexistent")
                .password("password")
                .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Tên tài khoản hoặc mật khẩu không chính xác"));
    }
}
