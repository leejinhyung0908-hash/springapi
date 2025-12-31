package site.protoa.api.auth_service.kakao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import site.protoa.api.auth_service.kakao.dto.KakaoTokenResponse;
import site.protoa.api.auth_service.kakao.dto.KakaoUserInfo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KakaoService {

    private final WebClient webClient;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.client-secret:}")
    private String kakaoClientSecret;

    @Autowired
    public KakaoService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 카카오 인가 URL 생성
     * 
     * @return 카카오 인가 URL
     */
    public String getAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", kakaoRestApiKey)
                .queryParam("redirect_uri", kakaoRedirectUri)
                .queryParam("response_type", "code")
                .toUriString();
    }

    /**
     * 인가 코드로 액세스 토큰 요청
     * 
     * @param code 인가 코드
     * @return KakaoTokenResponse
     */
    public KakaoTokenResponse getAccessToken(String code) {
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "authorization_code");
        formData.put("client_id", kakaoRestApiKey);
        formData.put("redirect_uri", kakaoRedirectUri);
        formData.put("code", code);

        // Client Secret이 있으면 추가
        if (kakaoClientSecret != null && !kakaoClientSecret.isEmpty()) {
            formData.put("client_secret", kakaoClientSecret);
        }

        String bodyString = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        return webClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(bodyString)
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block();
    }

    /**
     * 액세스 토큰으로 사용자 정보 요청
     * 
     * @param accessToken 액세스 토큰
     * @return KakaoUserInfo
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        return webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserInfo.class)
                .block();
    }
}
