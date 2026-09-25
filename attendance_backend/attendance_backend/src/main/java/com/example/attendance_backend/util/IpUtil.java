package com.example.attendance_backend.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Tiện ích bóc tách và đối chiếu địa chỉ IP mạng (hỗ trợ Nginx, Cloudflare, Multi-WAN, Subnet /24, IPv6 /64 và Local LAN Dev).
 */
public final class IpUtil {

    private IpUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Bóc tách địa chỉ Public IP thực tế của client từ request HTTP.
     * Ưu tiên các header qua Reverse Proxy / Cloudflare trước khi lấy RemoteAddr.
     */
    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        // 1. Cloudflare header
        String ip = request.getHeader("CF-Connecting-IP");

        // 2. Chuỗi X-Forwarded-For (Lấy IP đầu tiên của client thực)
        if (isEmptyOrUnknown(ip)) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip.trim())) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
            }
        }

        // 3. Nginx / Apache proxy header
        if (isEmptyOrUnknown(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        // 4. Remote address từ socket kết nối
        if (isEmptyOrUnknown(ip)) {
            ip = request.getRemoteAddr();
        }

        // Chuẩn hóa chuỗi IP
        if (ip == null || ip.trim().isEmpty()) {
            return "127.0.0.1";
        }

        ip = ip.trim();

        // Chuẩn hóa địa chỉ loopback IPv6 sang IPv4 cho môi trường phát triển (Dev)
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return "127.0.0.1";
        }

        return ip;
    }

    /**
     * Kiểm tra xem một IP có phải là loopback / localhost không.
     */
    public static boolean isLoopback(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        String clean = ip.trim();
        return "127.0.0.1".equals(clean) || "::1".equals(clean)
                || "0:0:0:0:0:0:0:1".equals(clean) || "localhost".equalsIgnoreCase(clean);
    }

    /**
     * Kiểm tra chuỗi có phải là định dạng IPv4 hợp lệ không.
     */
    public static boolean isIpv4(String ip) {
        if (ip == null) {
            return false;
        }
        return ip.trim().matches("^(\\d{1,3}\\.){3}\\d{1,3}$");
    }

    /**
     * Kiểm tra chuỗi có phải là địa chỉ IPv4 nội bộ / private (RFC 1918) không.
     */
    public static boolean isPrivateIpv4(String ip) {
        if (!isIpv4(ip)) {
            return false;
        }
        String clean = ip.trim();
        if (clean.startsWith("192.168.") || clean.startsWith("10.")) {
            return true;
        }
        if (clean.startsWith("172.")) {
            String[] parts = clean.split("\\.");
            try {
                int secondOctet = Integer.parseInt(parts[1]);
                return secondOctet >= 16 && secondOctet <= 31;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /**
     * Kiểm tra chuỗi có phải là định dạng IPv6 hợp lệ không.
     */
    public static boolean isIpv6(String ip) {
        return ip != null && ip.contains(":");
    }

    /**
     * Trích xuất prefix 64-bit đầu của địa chỉ IPv6 (tương đương 4 hextet đầu trong cùng mạng Wi-Fi router).
     */
    public static String extractIpv6Prefix64(String ipv6) {
        if (ipv6 == null || !ipv6.contains(":")) {
            return "";
        }
        String clean = ipv6.toLowerCase().trim();
        if (clean.contains("%")) {
            clean = clean.substring(0, clean.indexOf('%'));
        }
        String[] parts = clean.split(":");
        if (parts.length >= 4) {
            return parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3];
        }
        return clean;
    }

    /**
     * Kiểm tra tính hợp lệ của chuỗi MAC/BSSID của Router Wi-Fi.
     * Loại bỏ các giá trị dummy hoặc không hợp lệ do hệ điều hành trả về khi tắt GPS (ví dụ "02:00:00:00:00:00", "unknown").
     */
    public static boolean isValidBssid(String bssid) {
        if (bssid == null || bssid.trim().isEmpty()) {
            return false;
        }
        String clean = bssid.trim().toLowerCase();
        if ("unknown".equals(clean) || "null".equals(clean)
                || "02:00:00:00:00:00".equals(clean) || "00:00:00:00:00:00".equals(clean)) {
            return false;
        }
        return clean.matches("^([0-9a-f]{2}[:-]){5}([0-9a-f]{2})$");
    }

    /**
     * Lấy danh sách các địa chỉ IP mạng nội bộ/LAN (non-loopback) của máy chủ hiện tại.
     * Ví dụ: IP card Wi-Fi (192.168.1.116), card Ethernet.
     */
    public static List<String> getLocalHostNetworkIps() {
        List<String> localIps = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return localIps;
            }
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String hostAddr = addr.getHostAddress();
                        if (hostAddr.contains("%")) {
                            hostAddr = hostAddr.substring(0, hostAddr.indexOf('%'));
                        }
                        localIps.add(hostAddr);
                    }
                }
            }
        } catch (Exception ignored) {
            // Không ngắt luồng nếu có lỗi truy vấn interface
        }
        return localIps;
    }

    /**
     * Lấy địa chỉ IPv4 LAN chính của máy chủ (ưu tiên card Wi-Fi/Ethernet trong dải 192.168.x.x, 10.x.x.x).
     */
    public static String getPrimaryLocalIpv4() {
        List<String> ips = getLocalHostNetworkIps();
        for (String ip : ips) {
            if (isIpv4(ip) && isPrivateIpv4(ip)) {
                return ip;
            }
        }
        for (String ip : ips) {
            if (isIpv4(ip)) {
                return ip;
            }
        }
        return "127.0.0.1";
    }

    /**
     * Kiểm tra xem IP của sinh viên có cùng mạng với IP của giảng viên không.
     * Hỗ trợ:
     * 1. So khớp tuyệt đối (Exact match)
     * 2. Môi trường localhost / dev (127.0.0.1 tự động đối chiếu với card mạng LAN máy chủ)
     * 3. Dung sai dải mạng Subnet /24 cho IPv4 (khắc phục Multi-WAN và dải DHCP router)
     * 4. Prefix /64 cho IPv6
     */
    public static boolean isNetworkMatching(String clientIp, String hostIp) {
        if (clientIp == null || hostIp == null) {
            return false;
        }

        String cIp = clientIp.trim();
        String hIp = hostIp.trim();

        if (cIp.isEmpty() || hIp.isEmpty()) {
            return false;
        }

        // 1. So khớp chính xác hoàn toàn
        if (cIp.equalsIgnoreCase(hIp)) {
            return true;
        }

        // 2. Cả 2 đều là loopback
        if (isLoopback(cIp) && isLoopback(hIp)) {
            return true;
        }

        // 3. Nếu hostIp là loopback (phiên được tạo trên chính máy chủ backend)
        // và clientIp là IP thiết bị trong mạng LAN Wi-Fi:
        // Đối chiếu clientIp với các interface mạng nội bộ đang hoạt động của máy chủ
        if (isLoopback(hIp)) {
            List<String> serverIps = getLocalHostNetworkIps();
            for (String serverIp : serverIps) {
                if (isDirectNetworkMatch(cIp, serverIp)) {
                    return true;
                }
            }
        }

        // 4. Nếu clientIp là loopback
        if (isLoopback(cIp)) {
            List<String> serverIps = getLocalHostNetworkIps();
            for (String serverIp : serverIps) {
                if (isDirectNetworkMatch(hIp, serverIp)) {
                    return true;
                }
            }
        }

        // 5. Đối chiếu trực tiếp giữa 2 IP
        return isDirectNetworkMatch(cIp, hIp);
    }

    /**
     * Đối chiếu trực tiếp giữa 2 IP bất kỳ (hỗ trợ CIDR, Subnet /24 IPv4 và Prefix /64 IPv6).
     */
    public static boolean isDirectNetworkMatch(String cIp, String hIp) {
        if (cIp == null || hIp == null) {
            return false;
        }
        cIp = cIp.trim();
        hIp = hIp.trim();

        if (cIp.equalsIgnoreCase(hIp)) {
            return true;
        }

        // Xử lý hostIp dạng CIDR (ví dụ 192.168.1.0/24 hoặc 118.69.12.0/24)
        if (hIp.contains("/")) {
            String base = hIp.substring(0, hIp.indexOf('/'));
            int lastDot = base.lastIndexOf('.');
            if (lastDot > 0) {
                return cIp.startsWith(base.substring(0, lastDot + 1));
            }
        }

        // Xử lý hostIp kết thúc bằng .0 (ví dụ 192.168.1.0)
        if (hIp.endsWith(".0")) {
            String prefix = hIp.substring(0, hIp.length() - 1);
            return cIp.startsWith(prefix);
        }

        // Subnet /24 Matching cho IPv4 (cùng 3 octet đầu: ví dụ 192.168.1.116 và 192.168.1.50)
        if (isIpv4(cIp) && isIpv4(hIp)) {
            int cLastDot = cIp.lastIndexOf('.');
            int hLastDot = hIp.lastIndexOf('.');
            if (cLastDot > 0 && hLastDot > 0) {
                String cSubnet = cIp.substring(0, cLastDot);
                String hSubnet = hIp.substring(0, hLastDot);
                if (cSubnet.equalsIgnoreCase(hSubnet)) {
                    return true;
                }
            }
        }

        // Prefix /64 Matching cho IPv6 (cùng 4 hextet đầu trên cùng Router Wi-Fi)
        if (isIpv6(cIp) && isIpv6(hIp)) {
            String cPrefix = extractIpv6Prefix64(cIp);
            String hPrefix = extractIpv6Prefix64(hIp);
            if (!cPrefix.isEmpty() && cPrefix.equalsIgnoreCase(hPrefix)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isEmptyOrUnknown(String ip) {
        return ip == null || ip.trim().isEmpty() || "unknown".equalsIgnoreCase(ip.trim());
    }
}
