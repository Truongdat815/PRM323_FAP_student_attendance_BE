package com.example.attendance_backend.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Tiện ích sinh và xác thực mã TOTP (Time-based One-Time Password / HMAC-SHA256).
 * Dùng cho tính năng Dynamic QR Code điểm danh với chu kỳ làm mới mặc định 20 giây (tùy chỉnh được bởi giảng viên).
 */
public final class TotpUtil {

    public static final int DEFAULT_TIME_STEP_SECONDS = 20; // Mặc định 20 giây
    public static final int TIME_STEP_SECONDS = DEFAULT_TIME_STEP_SECONDS; // Backward compatibility
    public static final int DEFAULT_TOLERANCE_WINDOW = 1; // Cho phép sai lệch +/- 1 chu kỳ bù độ trễ mạng

    private TotpUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Sinh mã Dynamic QR Token hiện tại từ secret key theo chu kỳ mặc định (20s).
     */
    public static String generateCurrentToken(String secret) {
        return generateCurrentToken(secret, DEFAULT_TIME_STEP_SECONDS);
    }

    /**
     * Sinh mã Dynamic QR Token hiện tại từ secret key theo chu kỳ tùy chỉnh (timeStepSeconds).
     */
    public static String generateCurrentToken(String secret, int timeStepSeconds) {
        if (secret == null || secret.trim().isEmpty()) {
            return "";
        }
        int interval = timeStepSeconds > 0 ? timeStepSeconds : DEFAULT_TIME_STEP_SECONDS;
        long currentStep = System.currentTimeMillis() / 1000L / interval;
        return generateToken(secret.trim(), currentStep);
    }

    /**
     * Sinh token tại một bước thời gian (time step) cụ thể.
     */
    public static String generateToken(String secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);

            byte[] stepBytes = ByteBuffer.allocate(8).putLong(step).array();
            byte[] hash = mac.doFinal(stepBytes);

            // Lấy 4 byte đầu tiên và định dạng thành chuỗi Hex 8 ký tự viết hoa (VD: 4F3A8B12)
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02X", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh mã dynamic TOTP token: " + e.getMessage(), e);
        }
    }

    /**
     * Xác thực token theo chu kỳ mặc định (20s) và window dung sai.
     */
    public static boolean verifyToken(String secret, String token, int window) {
        return verifyToken(secret, token, DEFAULT_TIME_STEP_SECONDS, window);
    }

    /**
     * Xác thực token gửi lên từ máy sinh viên theo chu kỳ tùy chỉnh (timeStepSeconds) và window dung sai.
     */
    public static boolean verifyToken(String secret, String token, int timeStepSeconds, int window) {
        if (secret == null || token == null || token.trim().isEmpty()) {
            return false;
        }
        int interval = timeStepSeconds > 0 ? timeStepSeconds : DEFAULT_TIME_STEP_SECONDS;
        long currentStep = System.currentTimeMillis() / 1000L / interval;
        String cleanToken = token.trim();

        for (int i = -window; i <= window; i++) {
            String expected = generateToken(secret.trim(), currentStep + i);
            if (expected.equalsIgnoreCase(cleanToken)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lấy số giây còn lại trước khi token hiện tại chuyển sang chu kỳ mới theo chu kỳ mặc định (20s).
     */
    public static int getRemainingSeconds() {
        return getRemainingSeconds(DEFAULT_TIME_STEP_SECONDS);
    }

    /**
     * Lấy số giây còn lại trước khi token hiện tại chuyển sang chu kỳ mới theo chu kỳ tùy chỉnh.
     */
    public static int getRemainingSeconds(int timeStepSeconds) {
        int interval = timeStepSeconds > 0 ? timeStepSeconds : DEFAULT_TIME_STEP_SECONDS;
        long currentSeconds = System.currentTimeMillis() / 1000L;
        return (int) (interval - (currentSeconds % interval));
    }
}
