package site.protoa.api.auth_service.naver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import site.protoa.api.auth_service.naver.dto.NaverTokenResponse;
import site.protoa.api.auth_service.naver.dto.NaverUserInfo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NaverService {

    private final WebClient webClient;

    @Value("${naver.client-id}")
    private String naverClientId;

    @Value("${naver.client-secret}")
    private String naverClientSecret;

    @Value("${naver.redirect-uri}")
    private String naverRedirectUri;

    @Autowired
    public NaverService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 네이버 인가 URL 생성
     * 
     * @return 네이버 인가 URL
     */
    public String getAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        return UriComponentsBuilder.fromUriString("https://nid.naver.com/oauth2.0/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", naverClientId)
                .queryParam("redirect_uri", naverRedirectUri)
                .queryParam("state", state)
                .toUriString();
    }

    /**
     * 인가 코드로 액세스 토큰 요청
     * 
     * @param code  인가 코드
     * @param state 상태 값
     * @return NaverTokenResponse
     */
    public NaverTokenResponse getAccessToken(String code, String state) {
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "authorization_code");
        formData.put("client_id", naverClientId);
        formData.put("client_secret", naverClientSecret);
        formData.put("code", code);
        formData.put("state", state);

        String bodyString = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        return webClient.post()
                .uri("https://nid.naver.com/oauth2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(bodyString)
                .retrieve()
                .bodyToMono(NaverTokenResponse.class)
                .block();
    }

    /**
     * 액세스 토큰으로 사용자 정보 요청
     * 
     * @param accessToken 액세스 토큰
     * @return NaverUserInfo
     */
    public NaverUserInfo getUserInfo(String accessToken) {
        return webClient.get()
                .uri("https://openapi.naver.com/v1/nid/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(NaverUserInfo.class)
                .block();
    }
}
