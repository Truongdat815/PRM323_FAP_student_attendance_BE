package com.example.attendance_backend.security;

import com.example.attendance_backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationMs", 3600000L); // 1 hour
    }

    @Test
    void testGenerateAndValidateToken() {
        User user = User.builder()
                .userId("USR-12345678")
                .username("teststudent")
                .roleId("ROLE_STUDENT")
                .fapCode("SE192964")
                .build();

        String token = tokenProvider.generateToken(user);
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));

        assertEquals("teststudent", tokenProvider.getUsernameFromToken(token));
        assertEquals("USR-12345678", tokenProvider.getUserIdFromToken(token));
        assertEquals("ROLE_STUDENT", tokenProvider.getRoleFromToken(token));
    }

    @Test
    void testInvalidTokenReturnsFalse() {
        assertFalse(tokenProvider.validateToken("invalid.jwt.token"));
    }
}
