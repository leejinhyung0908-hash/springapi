package site.protoa.api.auth_service.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    private String secret;
    private Long expiration = 86400000L; // 기본 24시간
    private Long refreshExpiration = 604800000L; // 기본 7일
}
