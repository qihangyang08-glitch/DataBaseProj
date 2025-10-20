package com.example.taskplanning;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException; // 明确导入正确的SecurityException
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-in-ms}")
    private long jwtExpirationInMs; // 使用基本类型 long 更好

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 【核心改动】
     * 根据Spring Security的Authentication对象生成JWT Token.
     * 这是最标准的做法，因为它能从认证成功的结果中直接获取用户信息。
     *
     * @param authentication Spring Security认证成功的对象
     * @return 生成的JWT Token字符串
     */
    public String generateToken(Authentication authentication) {
        // 从Authentication对象中获取UserDetails，它包含了用户名等核心信息
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        String username = userPrincipal.getUsername();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        // 注意：如果你还想在Token中加入userId，需要让你的UserDetails实现类包含userId
        // 这是一个更高级的实践，我们暂时只用username作为主体(Subject)
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从JWT Token中提取用户名 (Subject)
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * 验证JWT Token是否有效
     *
     * @param token JWT Token
     * @return true如果Token有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parse(token);
            return true;
        } catch (SecurityException | MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * 获取JWT过期时间（毫秒）
     */
    public long getJwtExpirationInMs() {
        return jwtExpirationInMs;
    }

    // --- (以下方法在当前重构后的流程中不是必需的，但可以保留) ---

    public Long getUserIdFromToken(String token) {
        // ... (原代码保持不变，但请注意，如果generateToken不存userId，这里会报错)
        // 为了安全起见，我们暂时注释掉它，因为新版generateToken没存userId
        /*
        Claims claims = Jwts.parserBuilder()...
        return claims.get("userId", Long.class);
        */
        throw new UnsupportedOperationException("UserId is not included in the JWT token in this version.");
    }

    public Date getExpirationDateFromToken(String token) {
        // ... (原代码保持不变) ...
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }
}
