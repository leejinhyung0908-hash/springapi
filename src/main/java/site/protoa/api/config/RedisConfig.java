package site.protoa.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

@Configuration
public class RedisConfig {

    @Value("${UPSTASH_REDIS_URL:}")
    private String redisUrl;

    @Value("${UPSTASH_REDIS_TOKEN:}")
    private String redisToken;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        if (redisUrl != null && !redisUrl.isEmpty()) {
            try {
                // Upstash Redis URL 파싱
                // 형식 1: redis://default:token@host:port
                // 형식 2: rediss://default:token@host:port (SSL)
                // 형식 3: https://host:port (REST API)
                
                String host;
                int port = 6379;
                String password = redisToken;
                
                if (redisUrl.startsWith("redis://") || redisUrl.startsWith("rediss://")) {
                    // Redis 프로토콜 URL 파싱
                    URI uri = URI.create(redisUrl.replace("redis://", "http://").replace("rediss://", "https://"));
                    host = uri.getHost();
                    port = uri.getPort() > 0 ? uri.getPort() : 6379;
                    
                    // 사용자 정보에서 토큰 추출
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && userInfo.contains(":")) {
                        password = userInfo.split(":", 2)[1]; // 두 번째 부분이 토큰
                    }
                } else if (redisUrl.startsWith("https://") || redisUrl.startsWith("http://")) {
                    // REST API URL인 경우 (Upstash REST API는 별도 처리 필요)
                    // 일단 호스트만 추출
                    URI uri = URI.create(redisUrl);
                    host = uri.getHost();
                    port = uri.getPort() > 0 ? uri.getPort() : 6379;
                    // REST API는 password를 토큰으로 사용
                    if (redisToken != null && !redisToken.isEmpty()) {
                        password = redisToken;
                    }
                } else {
                    // 호스트만 있는 경우
                    host = redisUrl;
                }

                config.setHostName(host);
                config.setPort(port);
                if (password != null && !password.isEmpty()) {
                    config.setPassword(password);
                }
                
                System.out.println("✅ Redis 연결 설정 완료: " + host + ":" + port);
            } catch (Exception e) {
                // URL 파싱 실패 시 기본값 사용
                System.err.println("⚠️ Redis URL 파싱 실패, 기본 설정 사용: " + e.getMessage());
                config.setHostName("localhost");
                config.setPort(6379);
            }
        } else {
            // 환경 변수가 없으면 기본값 사용
            System.out.println("⚠️ Redis URL이 설정되지 않았습니다. 기본값 사용: localhost:6379");
            config.setHostName("localhost");
            config.setPort(6379);
        }

        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}

