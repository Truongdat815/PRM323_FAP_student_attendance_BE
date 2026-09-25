package com.example.attendance_backend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpUtilTest {

    private final String secret = "ATTENDANCE_SECRET_1234567890ABCDEF";

    @Test
    void testGenerateAndVerifyCurrentTokenDefault20Seconds() {
        String token = TotpUtil.generateCurrentToken(secret);
        assertNotNull(token);
        assertEquals(8, token.length());

        // Token hiện tại phải verify thành công với window = 0 và window = 1 theo mặc định (20s)
        assertTrue(TotpUtil.verifyToken(secret, token, 0));
        assertTrue(TotpUtil.verifyToken(secret, token, 1));
        assertTrue(TotpUtil.verifyToken(secret, token, 20, 1));
    }

    @Test
    void testGenerateAndVerifyCustomInterval() {
        // Kiểm tra với chu kỳ tùy chỉnh do giảng viên đặt (ví dụ 10s, 30s, 60s)
        String token10s = TotpUtil.generateCurrentToken(secret, 10);
        assertTrue(TotpUtil.verifyToken(secret, token10s, 10, 0));
        assertTrue(TotpUtil.verifyToken(secret, token10s, 10, 1));

        String token30s = TotpUtil.generateCurrentToken(secret, 30);
        assertTrue(TotpUtil.verifyToken(secret, token30s, 30, 0));
        assertTrue(TotpUtil.verifyToken(secret, token30s, 30, 1));
    }

    @Test
    void testVerifyTokenWithWindowTolerance() {
        long currentStep = System.currentTimeMillis() / 1000L / TotpUtil.DEFAULT_TIME_STEP_SECONDS;

        // Token của chu kỳ trước đó (currentStep - 1)
        String prevToken = TotpUtil.generateToken(secret, currentStep - 1);

        // Với window = 0 thì không khớp, nhưng window = 1 phải khớp (bù trễ mạng)
        assertFalse(TotpUtil.verifyToken(secret, prevToken, 0));
        assertTrue(TotpUtil.verifyToken(secret, prevToken, 1));

        // Token quá cũ (cách 3 chu kỳ) thì window = 1 phải từ chối
        String oldToken = TotpUtil.generateToken(secret, currentStep - 3);
        assertFalse(TotpUtil.verifyToken(secret, oldToken, 1));
    }

    @Test
    void testInvalidSecretOrToken() {
        assertFalse(TotpUtil.verifyToken(secret, "WRONGTOK", 1));
        assertFalse(TotpUtil.verifyToken(null, "WRONGTOK", 1));
        assertFalse(TotpUtil.verifyToken(secret, null, 1));
        assertFalse(TotpUtil.verifyToken(secret, "", 1));
    }

    @Test
    void testGetRemainingSeconds() {
        int remaining = TotpUtil.getRemainingSeconds();
        assertTrue(remaining >= 1 && remaining <= TotpUtil.DEFAULT_TIME_STEP_SECONDS);

        int remaining30 = TotpUtil.getRemainingSeconds(30);
        assertTrue(remaining30 >= 1 && remaining30 <= 30);
    }
}
