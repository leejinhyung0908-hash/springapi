package site.protoa.api.auth_service.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    @Autowired
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Access Token 생성
     * 
     * @param subject 사용자 식별자 (예: kakaoId)
     * @return Access Token 문자열
     */
    public String generateToken(String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        SecretKey key = getSecretKey();

        return Jwts.builder()
                .setSubject(subject)
                .claim("type", "access") // 토큰 타입 명시 (보안 강화)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * JWT 토큰에서 사용자 식별자 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 식별자
     */
    public String getSubjectFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * JWT 토큰 유효성 검증 (기본 - 하위 호환성 유지)
     * 
     * @param token JWT 토큰
     * @return 유효 여부
     * @deprecated validateAccessToken 또는 validateRefreshToken 사용 권장
     */
    @Deprecated
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Access Token 유효성 검증
     * 토큰 타입이 "access"인지 확인
     * 
     * @param token JWT 토큰
     * @return 유효 여부
     */
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get("type", String.class);
            return "access".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Refresh Token 유효성 검증
     * 토큰 타입이 "refresh"인지 확인
     * 
     * @param token JWT 토큰
     * @return 유효 여부
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get("type", String.class);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰 타입 조회
     * 
     * @param token JWT 토큰
     * @return 토큰 타입 ("access" 또는 "refresh")
     */
    public String getTokenType(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("type", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * JWT 토큰에서 Claims 추출
     * 
     * @param token JWT 토큰
     * @return Claims 객체
     */
    private Claims getClaimsFromToken(String token) {
        SecretKey key = getSecretKey();
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Secret Key 생성 (32바이트 이상 필요)
     * 
     * @return SecretKey 객체
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);

        // 최소 32바이트 필요
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            keyBytes = paddedKey;
        } else if (keyBytes.length > 32) {
            // 32바이트로 자르기 (HMAC-SHA256은 32바이트 사용)
            byte[] trimmedKey = new byte[32];
            System.arraycopy(keyBytes, 0, trimmedKey, 0, 32);
            keyBytes = trimmedKey;
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Refresh Token 생성
     * 
     * @param subject 사용자 식별자 (예: kakaoId)
     * @return Refresh Token 문자열
     */
    public String generateRefreshToken(String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshExpiration());

        SecretKey key = getSecretKey();

        return Jwts.builder()
                .setSubject(subject)
                .claim("type", "refresh") // 토큰 타입 명시 (보안 강화)
                .claim("jti", UUID.randomUUID().toString()) // JWT ID - 토큰 고유 식별자 (보안 강화)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Refresh Token에서 JTI (JWT ID) 추출
     * 
     * @param token Refresh Token
     * @return JTI 또는 null
     */
    public String getJtiFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("jti", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * JWT 만료 시간 반환 (밀리초)
     * 
     * @return 만료 시간 (밀리초)
     */
    public Long getExpiration() {
        return jwtProperties.getExpiration();
    }

    /**
     * Refresh Token 만료 시간 반환 (밀리초)
     * 
     * @return Refresh Token 만료 시간 (밀리초)
     */
    public Long getRefreshExpiration() {
        return jwtProperties.getRefreshExpiration();
    }
}
