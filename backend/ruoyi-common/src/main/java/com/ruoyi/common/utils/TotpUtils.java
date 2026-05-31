package com.ruoyi.common.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * Google Authenticator TOTP 工具类
 * 基于 RFC 6238 / RFC 4226，使用 Java 内置加密库，无需额外依赖
 */
public class TotpUtils {

    private static final int DIGITS = 6;
    private static final int STEP_SECONDS = 30;
    private static final String HMAC_ALGO = "HmacSHA1";

    /** Base32 字符表 */
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    /**
     * 生成随机 Base32 密钥（160 bit = 32位字符）
     */
    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * 生成 otpauth:// URL，供前端生成二维码
     *
     * @param issuer  应用名称（发行方）
     * @param account 用户账号
     * @param secret  Base32 密钥
     */
    public static String getOtpAuthUrl(String issuer, String account, String secret) {
        try {
            String encodedIssuer = java.net.URLEncoder.encode(issuer, "UTF-8");
            String encodedAccount = java.net.URLEncoder.encode(account, "UTF-8");
            return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                encodedIssuer, encodedAccount, secret, encodedIssuer
            );
        } catch (Exception e) {
            return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                issuer, account, secret, issuer
            );
        }
    }

    /**
     * 验证 TOTP 动态码（允许前后各 1 个时间步长的偏差）
     *
     * @param secret Base32 密钥
     * @param code   用户输入的 6 位动态码
     */
    public static boolean verifyCode(String secret, String code) {
        if (StringUtils.isEmpty(secret) || StringUtils.isEmpty(code)) {
            return false;
        }
        try {
            long currentStep = System.currentTimeMillis() / 1000L / STEP_SECONDS;
            for (int delta = -1; delta <= 1; delta++) {
                String expected = generateTotp(secret, currentStep + delta);
                if (expected.equals(code.trim())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String generateTotp(String secret, long step) throws Exception {
        byte[] key = base32Decode(secret.toUpperCase().replaceAll("\\s", ""));
        byte[] msg = new byte[8];
        for (int i = 7; i >= 0; i--) {
            msg[i] = (byte) (step & 0xFF);
            step >>= 8;
        }
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(key, HMAC_ALGO));
        byte[] hash = mac.doFinal(msg);
        int offset = hash[hash.length - 1] & 0x0F;
        long otp = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        return String.format("%0" + DIGITS + "d", otp % (long) Math.pow(10, DIGITS));
    }

    private static String base32Encode(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }

    private static byte[] base32Decode(String encoded) {
        int len = encoded.length();
        int outLen = len * 5 / 8;
        byte[] out = new byte[outLen];
        int buffer = 0, bitsLeft = 0, idx = 0;
        for (char c : encoded.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                if (idx < outLen) {
                    out[idx++] = (byte) ((buffer >> bitsLeft) & 0xFF);
                }
            }
        }
        return out;
    }
}
