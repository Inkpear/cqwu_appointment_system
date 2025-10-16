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


public final class JwtUtils {

    // 私有构造函数，防止该工具类被实例化
    private JwtUtils() {}

    /**
     * 根据 Base64 编码的密钥字符串，获取 SecretKey 对象
     */
    private static SecretKey getSecretKey(String base64Secret) {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
    }

    /**
     * 通用的 Token 创建方法
     */
    private static String createToken(Map<String, Object> claims, long expireTimeInMillis, String base64Secret) {
        JwtBuilder jwtBuilder = Jwts.builder()
                .header()
                .add("typ", "JWT")
                .add("alg", "HS256")
                .and()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireTimeInMillis))
                .signWith(getSecretKey(base64Secret), Jwts.SIG.HS256);

        return jwtBuilder.compact();
    }

    /**
     * 创建 Access Token
     */
    public static String createAccessToken(Map<String, Object> claims, JwtProperties properties) {
        return createToken(claims, properties.getAccessTokenExpireTime().toMillis(), properties.getSecret());
    }

    /**
     * 创建 Refresh Token
     */
    public static String createRefreshToken(String userId, JwtProperties properties) {
        Map<String, Object> claims = Map.of("userId", userId);
        return createToken(claims, properties.getRefreshTokenExpireTime().toMillis(), properties.getSecret());
    }

    /**
     * 解析 JWT
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
     * 运行这个 main 方法来生成一个新的密钥。
     */
    public static void main(String[] args) {
        SecretKey key = Jwts.SIG.HS256.key().build();
        String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("请将以下生成的密钥复制到 application.properties 的 jwt.secret 中:");
        System.out.println(encodedKey);
    }
}