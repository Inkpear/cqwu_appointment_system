package com.cqwu.cqwu_appointment_system.common.utils;

import com.cqwu.cqwu_appointment_system.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * @description JWT（JSON Web Token）工具类。
 *              负责生成、解析和验证JWT，以及生成安全的密钥。
 */
public final class JwtUtils {

    // 私有构造函数，防止该工具类被实例化
    private JwtUtils() {
    }

    /**
     * 根据 Base64 编码的密钥字符串，获取 SecretKey 对象。
     */
    private static SecretKey getSecretKey(String base64Secret) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Secret);
        return Keys.hmacShaKeyFor(decodedKey);
    }

    /**
     * 通用的 Token 创建方法。
     * 
     * @param claims             载荷
     * @param expireTimeInMillis 过期时长（毫秒）
     * @param base64Secret       Base64编码的密钥
     * @return 生成的JWT字符串
     */
    private static String createToken(Map<String, Object> claims, long expireTimeInMillis, String base64Secret) {
        JwtBuilder jwtBuilder = Jwts.builder()
                .header()
                .add("typ", "JWT")
                .add("alg", "HS256")
                .and()
                .claims(claims)
                .id(UUID.randomUUID().toString()) // jti: JWT ID，防止重放
                .issuedAt(new Date()) // iat: 签发时间
                .expiration(new Date(System.currentTimeMillis() + expireTimeInMillis)) // exp: 过期时间
                .signWith(getSecretKey(base64Secret), Jwts.SIG.HS256); // 签名算法

        return jwtBuilder.compact();
    }

    /**
     * 创建 Access Token。
     */
    public static String createAccessToken(Map<String, Object> claims, JwtProperties properties) {
        return createToken(claims, properties.getAccessTokenExpireTime().toMillis(), properties.getSecret());
    }

    /**
     * 创建 Refresh Token。
     */
    public static String createRefreshToken(String userId, JwtProperties properties) {
        Map<String, Object> claims = Map.of("userId", userId);
        return createToken(claims, properties.getRefreshTokenExpireTime().toMillis(), properties.getSecret());
    }

    /**
     * 解析并验证 JWT。如果签名无效、Token过期或格式错误，将抛出相应的JwtException。
     * 
     * @param token        JWT字符串
     * @param base64Secret Base64编码的密钥
     * @return 包含载荷信息的 Claims 对象
     */
    public static Claims parseJwt(String token, String base64Secret) {
        return Jwts.parser()
                .verifyWith(getSecretKey(base64Secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * (独立运行的密钥生成器)
     * 运行此 main 方法来生成一个新的、安全的 Base64 编码密钥。
     */
    public static void main(String[] args) {
        SecretKey key = Jwts.SIG.HS256.key().build();
        String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("请将以下生成的密钥复制到 application.properties 的 jwt.secret 中:");
        System.out.println(encodedKey);
    }
}