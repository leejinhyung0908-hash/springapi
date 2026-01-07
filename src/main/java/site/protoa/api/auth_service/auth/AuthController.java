package site.protoa.api.auth_service.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.protoa.api.auth_service.jwt.JwtTokenProvider;
import site.protoa.api.auth_service.token.AccessTokenService;
import site.protoa.api.auth_service.token.RefreshTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    @Autowired
    public AuthController(JwtTokenProvider jwtTokenProvider, AccessTokenService accessTokenService,
            RefreshTokenService refreshTokenService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 인증 상태 확인 및 사용자 정보 반환
     * 쿠키에서 JWT 토큰을 읽어 검증하고, 사용자 ID를 반환
     * 
     * @param request HttpServletRequest (쿠키 읽기용)
     * @return 사용자 정보 또는 에러 응답
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // 쿠키에서 토큰 추출
            String token = extractTokenFromCookie(request);

            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Unauthorized",
                                "message", "인증이 필요합니다."));
            }

            // 토큰 검증
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Unauthorized",
                                "message", "유효하지 않은 토큰입니다."));
            }

            // 토큰에서 사용자 ID 추출
            String userId = jwtTokenProvider.getSubjectFromToken(token);

            // Redis에서 Access Token 확인 (저장된 토큰과 일치하는지 확인)
            String storedToken = accessTokenService.getToken(userId);
            if (storedToken == null || !storedToken.equals(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Unauthorized",
                                "message", "저장된 토큰과 일치하지 않습니다."));
            }

            // 사용자 정보 반환
            // TODO: 실제 사용자 정보를 DB에서 조회하거나, 소셜 로그인 정보를 저장/조회하는 로직 추가 필요
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", userId);
            // userInfo.put("email", user.getEmail());
            // userInfo.put("name", user.getName());
            // userInfo.put("profileImage", user.getProfileImage());

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", "서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 토큰 갱신 (세션 회전 포함)
     * Refresh Token으로 새로운 Access Token과 Refresh Token 발급
     * 이전 Refresh Token은 무효화되어 재사용 불가능
     * 
     * @param request  HttpServletRequest (쿠키 읽기용)
     * @param response HttpServletResponse (쿠키 설정용)
     * @return 새로운 Access Token 또는 에러 응답
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // 쿠키에서 Refresh Token 추출
            String refreshToken = extractRefreshTokenFromCookie(request);

            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Unauthorized",
                                "message", "Refresh Token이 필요합니다."));
            }

            // Refresh Token 검증
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Unauthorized",
                                "message", "유효하지 않은 Refresh Token입니다."));
            }

            // DB에서 Refresh Token 확인
            if (!refreshTokenService.existsToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Unauthorized",
                                "message", "저장된 Refresh Token과 일치하지 않습니다."));
            }

            // Refresh Token에서 사용자 ID 추출
            String userId = jwtTokenProvider.getSubjectFromToken(refreshToken);

            // 세션 회전: 이전 Refresh Token 무효화 (보안 강화)
            refreshTokenService.deleteToken(refreshToken);

            // 새로운 Access Token 발급
            String newAccessToken = jwtTokenProvider.generateToken(userId);

            // 새로운 Refresh Token 발급 (세션 회전)
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

            // 새로운 Access Token을 Redis에 저장
            long accessTokenExpirationSeconds = jwtTokenProvider.getExpiration() / 1000;
            accessTokenService.saveToken(userId, newAccessToken, accessTokenExpirationSeconds);

            // 새로운 Refresh Token을 DB에 저장
            long refreshTokenExpirationSeconds = jwtTokenProvider.getRefreshExpiration() / 1000;
            refreshTokenService.saveToken(userId, newRefreshToken, refreshTokenExpirationSeconds);

            // 새로운 Access Token을 쿠키에 저장
            ResponseCookie accessTokenCookie = ResponseCookie.from("Authorization", newAccessToken)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(jwtTokenProvider.getExpiration() / 1000)
                    .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

            // 새로운 Refresh Token을 쿠키에 저장 (세션 회전)
            ResponseCookie refreshTokenCookie = ResponseCookie.from("RefreshToken", newRefreshToken)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(jwtTokenProvider.getRefreshExpiration() / 1000)
                    .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "토큰이 갱신되었습니다.");

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", "토큰 갱신 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 로그아웃
     * 쿠키에서 Access Token과 Refresh Token 삭제
     * 
     * @param request  HttpServletRequest
     * @param response HttpServletResponse (쿠키 삭제용)
     * @return 로그아웃 성공 응답
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // 쿠키에서 토큰 추출
            String token = extractTokenFromCookie(request);
            String refreshToken = extractRefreshTokenFromCookie(request);

            // Redis에서 Access Token 삭제
            if (token != null) {
                try {
                    String userId = jwtTokenProvider.getSubjectFromToken(token);
                    if (userId != null) {
                        accessTokenService.deleteToken(userId);
                    }
                } catch (Exception e) {
                    // 토큰 파싱 실패 시 무시
                }
            }

            // DB에서 Refresh Token 삭제
            if (refreshToken != null) {
                try {
                    refreshTokenService.deleteToken(refreshToken);
                } catch (Exception e) {
                    // 토큰 삭제 실패 시 무시
                }
            }

            // Access Token 쿠키 삭제 (ResponseCookie로 SameSite 명시적 설정)
            ResponseCookie accessTokenCookie = ResponseCookie.from("Authorization", "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(0) // 즉시 삭제
                    .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite) // Lax, Strict, None
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

            // Refresh Token 쿠키 삭제 (ResponseCookie로 SameSite 명시적 설정)
            ResponseCookie refreshTokenCookie = ResponseCookie.from("RefreshToken", "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(0) // 즉시 삭제
                    .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite) // Lax, Strict, None
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "로그아웃되었습니다.");

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", false);
            responseBody.put("error", "로그아웃 처리 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseBody);
        }
    }

    /**
     * 쿠키에서 Authorization 토큰 추출
     * 
     * @param request HttpServletRequest
     * @return JWT 토큰 또는 null
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Authorization".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 쿠키에서 Refresh Token 추출
     * 
     * @param request HttpServletRequest
     * @return Refresh Token 또는 null
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("RefreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
// tokenss
