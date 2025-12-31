package site.protoa.api.auth_service.kakao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.protoa.api.auth_service.jwt.JwtTokenProvider;
import site.protoa.api.auth_service.kakao.dto.KakaoUserInfo;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/kakao")
public class KakaoController {

        private final KakaoService kakaoService;
        private final JwtTokenProvider jwtTokenProvider;

        @Value("${frontend.login-callback-url:http://localhost:3000}")
        private String frontendCallbackUrl;

        @Value("${frontend.login-success-path:/}")
        private String loginSuccessPath;

        @Value("${cookie.secure:false}")
        private boolean cookieSecure;

        @Value("${cookie.same-site:Lax}")
        private String cookieSameSite;

        @Autowired
        public KakaoController(KakaoService kakaoService, JwtTokenProvider jwtTokenProvider) {
                this.kakaoService = kakaoService;
                this.jwtTokenProvider = jwtTokenProvider;
        }

        /**
         * 카카오 인가 URL 생성 및 반환
         * 프론트엔드에서 이 URL로 리다이렉트
         * 
         * @return 카카오 인가 URL
         */
        @GetMapping("/login")
        public ResponseEntity<Map<String, String>> getKakaoAuthUrl() {
                String authUrl = kakaoService.getAuthorizationUrl();
                Map<String, String> response = new HashMap<>();
                response.put("authUrl", authUrl);
                return ResponseEntity.ok(response);
        }

        /**
         * 카카오 인가 코드 콜백 처리
         * 1. 인가 코드로 액세스 토큰 요청
         * 2. 액세스 토큰으로 사용자 정보 요청
         * 3. JWT 발급 (카카오 ID 기반)
         * 4. JWT를 쿠키에 저장하고 프론트엔드로 리다이렉트
         * 
         * @param code     카카오 인가 코드
         * @param response HttpServletResponse (쿠키 설정용)
         * @return 프론트엔드로 리다이렉트 (쿠키에 JWT 토큰 포함)
         */
        @GetMapping("/callback")
        public ResponseEntity<?> kakaoCallback(
                        @RequestParam("code") String code,
                        @RequestHeader(value = "X-Frontend-Callback-Url", required = false) String frontendCallbackUrlFromHeader,
                        HttpServletResponse response) {
                try {
                        // 1. 인가 코드로 액세스 토큰 요청
                        var tokenResponse = kakaoService.getAccessToken(code);
                        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(Map.of("success", false, "message", "카카오 토큰 요청 실패"));
                        }

                        String accessToken = tokenResponse.getAccessToken();

                        // 2. 액세스 토큰으로 사용자 정보 요청
                        KakaoUserInfo userInfo = kakaoService.getUserInfo(accessToken);
                        if (userInfo == null || userInfo.getId() == null) {
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(Map.of("success", false, "message", "카카오 사용자 정보 조회 실패"));
                        }

                        // 3. 카카오 ID 추출
                        String kakaoId = userInfo.getId().toString();

                        // 4. JWT 및 Refresh Token 발급 (카카오 ID를 subject로 사용)
                        String jwt = jwtTokenProvider.generateToken(kakaoId);
                        String refreshToken = jwtTokenProvider.generateRefreshToken(kakaoId);

                        // 4-1. 백엔드 터미널에 로그 출력 (보안: 토큰 전체는 출력하지 않음)
                        String timestamp = LocalDateTime.now()
                                        .format(DateTimeFormatter.ofPattern("yyyy. MM. dd. a h:mm:ss", Locale.KOREAN));

                        System.out.println("\n" + "=".repeat(60));
                        System.out.println("[" + timestamp + "] 🔹 카카오 로그인 성공");
                        System.out.println("User ID: " + kakaoId);
                        System.out.println("Token Length: " + jwt.length());
                        System.out.println("Refresh Token Length: " + refreshToken.length());
                        System.out.println("=".repeat(60) + "\n");

                        // 5. Access Token을 쿠키에 저장 (ResponseCookie로 SameSite 명시적 설정)
                        ResponseCookie accessTokenCookie = ResponseCookie.from("Authorization", jwt)
                                        .httpOnly(true) // JavaScript 접근 차단 (XSS 방지)
                                        .secure(cookieSecure) // HTTPS에서만 전송 (프로덕션: true)
                                        .path("/") // 모든 경로에서 사용 가능
                                        .maxAge(jwtTokenProvider.getExpiration() / 1000) // 초 단위
                                        .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite) // Lax,
                                                                                                           // Strict,
                                                                                                           // None
                                        .build();
                        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

                        // 5-1. Refresh Token을 쿠키에 저장 (ResponseCookie로 SameSite 명시적 설정)
                        ResponseCookie refreshTokenCookie = ResponseCookie.from("RefreshToken", refreshToken)
                                        .httpOnly(true) // JavaScript 접근 차단 (XSS 방지)
                                        .secure(cookieSecure) // HTTPS에서만 전송 (프로덕션: true)
                                        .path("/") // 모든 경로에서 사용 가능
                                        .maxAge(jwtTokenProvider.getRefreshExpiration() / 1000) // 초 단위 (더 긴 만료 시간)
                                        .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite) // Lax,
                                                                                                           // Strict,
                                                                                                           // None
                                        .build();
                        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

                        // 6. 프론트엔드 콜백 페이지로 리다이렉트 (토큰 없는 URL)
                        // Gateway에서 전달한 헤더가 있으면 우선 사용, 없으면 환경 변수 사용
                        String callbackUrl = (frontendCallbackUrlFromHeader != null
                                        && !frontendCallbackUrlFromHeader.isEmpty())
                                                        ? frontendCallbackUrlFromHeader
                                                        : frontendCallbackUrl;

                        // URL에서 경로 부분 제거 (프로토콜 + 호스트 + 포트만 유지)
                        try {
                                java.net.URL url = new java.net.URL(callbackUrl);
                                callbackUrl = url.getProtocol() + "://" + url.getHost()
                                                + (url.getPort() != -1 ? ":" + url.getPort() : "");
                        } catch (Exception e) {
                                // URL 파싱 실패 시 그대로 사용
                        }

                        String redirectUrl = callbackUrl + "/login/kakao/callback";

                        // 디버깅: frontendCallbackUrl과 redirectUrl 값 확인
                        System.out.println("[KakaoController] frontendCallbackUrlFromHeader: "
                                        + frontendCallbackUrlFromHeader);
                        System.out.println("[KakaoController] frontendCallbackUrl (env): " + frontendCallbackUrl);
                        System.out.println("[KakaoController] callbackUrl (used): " + callbackUrl);
                        System.out.println("[KakaoController] redirectUrl: " + redirectUrl);

                        return ResponseEntity.status(HttpStatus.FOUND)
                                        .header(HttpHeaders.LOCATION, redirectUrl)
                                        .build();

                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("success", false, "message",
                                                        "카카오 로그인 처리 중 오류: " + e.getMessage()));
                }
        }
}
