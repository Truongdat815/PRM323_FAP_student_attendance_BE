package com.example.attendance_backend.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class IpUtilTest {

    @Test
    void testExtractClientIpFromCfConnectingIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "118.69.12.50");
        request.addHeader("X-Forwarded-For", "192.168.1.1");
        request.setRemoteAddr("10.0.0.1");

        String ip = IpUtil.extractClientIp(request);
        assertEquals("118.69.12.50", ip);
    }

    @Test
    void testExtractClientIpFromXForwardedForMultipleIps() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "118.69.12.99, 10.0.0.1, 172.16.0.1");
        request.setRemoteAddr("127.0.0.1");

        String ip = IpUtil.extractClientIp(request);
        assertEquals("118.69.12.99", ip);
    }

    @Test
    void testExtractClientIpNormalizeLoopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        String ip = IpUtil.extractClientIp(request);
        assertEquals("127.0.0.1", ip);
    }

    @Test
    void testIsNetworkMatchingExact() {
        assertTrue(IpUtil.isNetworkMatching("118.69.12.50", "118.69.12.50"));
        assertTrue(IpUtil.isNetworkMatching("127.0.0.1", "127.0.0.1"));
    }

    @Test
    void testIsNetworkMatchingSubnet24() {
        // Cùng dải /24 (khắc phục Multi-WAN trường học hoặc Wi-Fi LAN gia đình)
        assertTrue(IpUtil.isNetworkMatching("118.69.12.50", "118.69.12.80"));
        assertTrue(IpUtil.isNetworkMatching("14.161.45.10", "14.161.45.99"));
        assertTrue(IpUtil.isNetworkMatching("192.168.1.50", "192.168.1.116"));

        // Khác dải /24
        assertFalse(IpUtil.isNetworkMatching("118.69.12.50", "118.69.13.50"));
        assertFalse(IpUtil.isNetworkMatching("14.161.45.10", "171.244.8.99"));
        assertFalse(IpUtil.isNetworkMatching("192.168.1.50", "192.168.2.50"));
    }

    @Test
    void testIsNetworkMatchingCidrAndPrefix() {
        assertTrue(IpUtil.isNetworkMatching("192.168.1.15", "192.168.1.0/24"));
        assertTrue(IpUtil.isNetworkMatching("192.168.1.15", "192.168.1.0"));
        assertFalse(IpUtil.isNetworkMatching("192.168.2.15", "192.168.1.0/24"));
    }

    @Test
    void testIsNetworkMatchingIpv6Prefix64() {
        // Cùng dải IPv6 /64 (chung router Wi-Fi)
        assertTrue(IpUtil.isNetworkMatching(
                "2402:800:6370:ce02:66c:89c5:3217:ef6f",
                "2402:800:6370:ce02:abcd:1234:5678:90ab"
        ));

        // Khác dải IPv6 /64
        assertFalse(IpUtil.isNetworkMatching(
                "2402:800:6370:ce02:66c:89c5:3217:ef6f",
                "2402:800:6370:ffff:66c:89c5:3217:ef6f"
        ));
    }

    @Test
    void testIsValidBssid() {
        assertTrue(IpUtil.isValidBssid("34:2c:c4:a1:8b:90"));
        assertTrue(IpUtil.isValidBssid("34-2C-C4-A1-8B-90"));
        assertFalse(IpUtil.isValidBssid("02:00:00:00:00:00"));
        assertFalse(IpUtil.isValidBssid("00:00:00:00:00:00"));
        assertFalse(IpUtil.isValidBssid("unknown"));
        assertFalse(IpUtil.isValidBssid("null"));
        assertFalse(IpUtil.isValidBssid(""));
        assertFalse(IpUtil.isValidBssid(null));
    }

    @Test
    void testLoopbackResolutionAgainstServerInterfaces() {
        // Lấy IP LAN chính của máy chủ
        String primaryLan = IpUtil.getPrimaryLocalIpv4();
        assertNotNull(primaryLan);

        // Nếu máy chủ có card mạng LAN (ví dụ 192.168.1.116),
        // bất kỳ IP nào cùng dải /24 đối chiếu với 127.0.0.1 đều phải match!
        if (primaryLan.contains(".")) {
            int lastDot = primaryLan.lastIndexOf('.');
            String clientIpInSameSubnet = primaryLan.substring(0, lastDot + 1) + "222";
            assertTrue(IpUtil.isNetworkMatching(clientIpInSameSubnet, "127.0.0.1"));
        }
    }
}
