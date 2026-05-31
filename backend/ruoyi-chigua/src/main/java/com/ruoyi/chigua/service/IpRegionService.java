package com.ruoyi.chigua.service;

import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * IP 归属地查询服务（基于 ip2region 离线库）
 * xdb 文件从 classpath 加载，打包进 jar 即可使用，无需额外部署。
 */
@Service
public class IpRegionService {

    private static final Logger logger = LoggerFactory.getLogger(IpRegionService.class);

    private static final String XDB_RESOURCE = "/ip2region.xdb";

    /**
     * 内存模式 Searcher：启动时创建一次，线程安全，全局复用。
     * newWithBuffer 模式官方明确支持多线程并发查询。
     */
    private Searcher searcher;

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream(XDB_RESOURCE)) {
            if (is == null) {
                logger.error("ip2region.xdb 未找到，请确认文件已放入 resources 目录");
                return;
            }
            byte[] xdbBuffer = readAllBytes(is);
            searcher = Searcher.newWithBuffer(xdbBuffer);
            logger.info("ip2region.xdb 加载完成，大小: {} KB", xdbBuffer.length / 1024);
        } catch (Exception e) {
            logger.error("加载 ip2region.xdb 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 查询 IP 的地区信息字符串
     * 返回格式：国家|区域|省份|城市|ISP（例：中国|0|广东省|深圳市|电信）
     * 失败返回 null
     */
    public String search(String ip) {
        if (searcher == null || ip == null || ip.isEmpty()) {
            return null;
        }
        try {
            return searcher.search(ip);
        } catch (Exception e) {
            logger.debug("ip2region 查询失败: ip={}, error={}", ip, e.getMessage());
            return null;
        }
    }

    /**
     * 判断 IP 是否属于中国大陆
     *
     * 重要：ip2region 的 .xdb 数据库主要支持 IPv4，对 IPv6 查询会返回 null。
     * 历史上的 fallback "查不到 → 默认国内" 在 IPv4 场景没问题，但会把所有
     * 海外 IPv6 用户误判成 CN（海外 IPv6 普及率远高于国内），因此对 IPv6
     * 必须单独处理：仅匹配明确的中国电信/联通/移动/教育网 IPv6 段才判 CN，
     * 其余 IPv6 默认 OS。
     */
    public boolean isChina(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true; // 无法判断时默认国内（极少出现）
        }
        // 内网/私有 IP 直接认为是国内
        if (isPrivateIp(ip)) {
            return true;
        }
        // IPv6 走专用判定（ip2region 对 IPv6 不友好）
        if (isIpv6(ip)) {
            return isChineseIpv6(ip);
        }
        // IPv4 走 ip2region
        String region = search(ip);
        if (region == null) {
            return true; // IPv4 查询失败默认国内（罕见，新分配段）
        }
        // region 格式：国家|区域|省份|城市|ISP
        // 中国大陆的国家字段固定为"中国"，港澳台为"香港"/"澳门"/"台湾"
        String country = region.split("\\|")[0];
        return "中国".equals(country);
    }

    /** 简单判断是否为 IPv6 字符串（含 ":"） */
    private boolean isIpv6(String ip) {
        return ip != null && ip.indexOf(':') >= 0;
    }

    /**
     * 判断 IPv6 是否为中国大陆运营商分配段
     * 仅匹配主要的国内 IPv6 段，其余一律视为海外。
     *
     * 主要国内 IPv6 段（来源 APNIC 官方分配）：
     *   2408::/20            China Unicom（中国联通）
     *   240e::/20            China Telecom（中国电信）
     *   2409::/20            China Mobile（中国移动）
     *   2400:da00::/32       China Telecom 骨干
     *   2001:da8::/32        CERNET 教育网
     *   2001:cc0::/32        CSTNET 中科院
     *
     * 注意：判定逻辑只看 IPv6 前缀的开头几位，不做完整的 CIDR 计算
     * （够用且零依赖；APNIC 段分配相对稳定，未来如需扩展再加段）。
     */
    private boolean isChineseIpv6(String ip) {
        if (ip == null) return false;
        String lower = ip.trim().toLowerCase();
        // 去掉可能的 "[...]" 包裹和 zone id "%eth0"
        if (lower.startsWith("[") && lower.endsWith("]")) {
            lower = lower.substring(1, lower.length() - 1);
        }
        int pct = lower.indexOf('%');
        if (pct >= 0) {
            lower = lower.substring(0, pct);
        }
        // /20 段：取前缀 5 个 hex 字符里的高 20 bit
        // 简化：直接匹配最常见前缀写法
        // 2408::/20 联通  → 2408 开头任意第 5 位 0~f 都属于 /20，覆盖 2408..240f 中的 2408
        if (lower.startsWith("2408:")) return true;
        // 240e::/20 电信
        if (lower.startsWith("240e:")) return true;
        // 2409::/20 移动
        if (lower.startsWith("2409:")) return true;
        // 2400:da00::/32 电信骨干
        if (lower.startsWith("2400:da00:") || lower.startsWith("2400:da0:")
                || lower.startsWith("2400:da00")) return true;
        // 2001:da8::/32 教育网
        if (lower.startsWith("2001:da8:") || lower.startsWith("2001:0da8:")) return true;
        // 2001:cc0::/32 中科院
        if (lower.startsWith("2001:cc0:") || lower.startsWith("2001:0cc0:")) return true;
        return false;
    }

    /**
     * 从 HttpServletRequest 中提取真实客户端 IP
     * 依次检查：CF-Connecting-IP → True-Client-IP → X-Forwarded-For → X-Real-IP → RemoteAddr
     *
     * 重要：必须优先读 Cloudflare 设置的 CF-Connecting-IP / True-Client-IP，
     *       否则当流量经过 Cloudflare 时，X-Real-IP/$remote_addr 会被替换为 Cloudflare 节点 IP
     *       （多数 CF 节点在美国/欧洲，但部分边缘节点位于中国大陆，会导致 IP 归属判断错误）。
     */
    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // 1) Cloudflare 透传的真实客户端 IP（最可靠）
        String ip = request.getHeader("CF-Connecting-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }
        ip = request.getHeader("True-Client-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }
        // 2) 标准代理头：X-Forwarded-For 可能含多个 IP，取最左（最初的客户端）
        ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip.trim());
    }

    /** 兼容 Java 8：读取 InputStream 全部字节 */
    private byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    /** 判断是否为私有/内网 IP（RFC 1918 + loopback） */
    private boolean isPrivateIp(String ip) {
        // 10.0.0.0/8
        if (ip.startsWith("10.")) return true;
        // 192.168.0.0/16
        if (ip.startsWith("192.168.")) return true;
        // 172.16.0.0/12 → 172.16.x.x ~ 172.31.x.x，逐段精确匹配
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) return true;
                } catch (NumberFormatException ignored) {}
            }
        }
        // loopback
        return ip.equals("127.0.0.1") || ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1");
    }
}
