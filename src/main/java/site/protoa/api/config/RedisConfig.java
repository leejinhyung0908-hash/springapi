package site.protoa.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import java.time.Duration;

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
        boolean useSsl = false;

        if (redisUrl != null && !redisUrl.isEmpty()) {
            try {
                // Upstash Redis URL 파싱
                // 형식 1: redis://default:token@host:port
                // 형식 2: rediss://default:token@host:port (SSL)

                String host;
                int port = 6379;
                String password = redisToken;

                if (redisUrl.startsWith("redis://") || redisUrl.startsWith("rediss://")) {
                    // SSL 여부 확인
                    useSsl = redisUrl.startsWith("rediss://");

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
                    // REST API URL인 경우는 Redis 프로토콜이 아니므로 에러
                    throw new IllegalArgumentException(
                            "REST API URL은 지원하지 않습니다. Redis 프로토콜 URL을 사용하세요: redis:// 또는 rediss://");
                } else {
                    // 호스트만 있는 경우
                    host = redisUrl;
                }

                config.setHostName(host);
                config.setPort(port);
                if (password != null && !password.isEmpty()) {
                    config.setPassword(password);
                }

                System.out.println("✅ Redis 연결 설정:");
                System.out.println("   Host: " + host);
                System.out.println("   Port: " + port);
                System.out.println("   SSL: " + useSsl);
                System.out.println("   Password: " + (password != null && !password.isEmpty() ? "***설정됨***" : "없음"));
            } catch (Exception e) {
                // URL 파싱 실패 시 에러 출력
                System.err.println("❌ Redis URL 파싱 실패: " + e.getMessage());
                System.err.println("   Redis URL: "
                        + (redisUrl != null ? redisUrl.substring(0, Math.min(50, redisUrl.length())) + "..." : "null"));
                e.printStackTrace();
                throw new RuntimeException("Redis 연결 설정 실패: " + e.getMessage(), e);
            }
        } else {
            // 환경 변수가 없으면 에러
            System.err.println("❌ Redis URL이 설정되지 않았습니다!");
            System.err.println("   UPSTASH_REDIS_URL 환경 변수를 설정하세요.");
            throw new RuntimeException("Redis URL이 설정되지 않았습니다. UPSTASH_REDIS_URL 환경 변수를 확인하세요.");
        }

        // LettuceClientConfiguration 생성
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(Duration.ofSeconds(10))
                                .build())
                        .timeoutOptions(TimeoutOptions.builder()
                                .fixedTimeout(Duration.ofSeconds(10))
                                .build())
                        .build())
                .commandTimeout(Duration.ofSeconds(10))
                .build();

        // LettuceConnectionFactory 생성 (설정과 클라이언트 설정 전달)
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);

        // 연결 초기화
        factory.afterPropertiesSet();

        // 연결 테스트 (비동기 초기화를 위해 약간의 지연)
        try {
            Thread.sleep(500); // 연결 초기화 대기
            factory.getConnection().ping();
            System.out.println("✅ Redis 연결 테스트 성공!");
        } catch (Exception e) {
            System.err.println("❌ Redis 연결 테스트 실패: " + e.getMessage());
            System.err.println("   호스트: " + config.getHostName());
            System.err.println("   포트: " + config.getPort());
            System.err.println("   SSL: " + useSsl);
            e.printStackTrace();
            // 연결 테스트 실패해도 빈은 생성 (런타임에 재시도 가능)
            System.err.println("⚠️  경고: Redis 연결 테스트 실패했지만 계속 진행합니다.");
        }

        return factory;
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
