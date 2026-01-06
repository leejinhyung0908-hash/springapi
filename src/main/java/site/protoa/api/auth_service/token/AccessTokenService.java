package site.protoa.api.auth_service.token;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Access Token을 Upstash Redis에 저장/조회/삭제하는 서비스
 */
@Service
public class AccessTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String TOKEN_PREFIX = "access_token:";

    @Autowired
    public AccessTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Access Token을 Redis에 저장
     * 
     * @param userId            사용자 ID
     * @param token             Access Token
     * @param expirationSeconds 만료 시간 (초)
     */
    public void saveToken(String userId, String token, long expirationSeconds) {
        String key = TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, token, expirationSeconds, TimeUnit.SECONDS);
    }

    /**
     * Access Token을 Redis에서 조회
     * 
     * @param userId 사용자 ID
     * @return Access Token 또는 null
     */
    public String getToken(String userId) {
        String key = TOKEN_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Access Token을 Redis에서 삭제
     * 
     * @param userId 사용자 ID
     */
    public void deleteToken(String userId) {
        String key = TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
    }

    /**
     * Access Token 존재 여부 확인
     * 
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    public boolean existsToken(String userId) {
        String key = TOKEN_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 토큰으로 사용자 ID 조회 (역방향 조회)
     * 주의: 이 방법은 모든 키를 스캔하므로 성능이 좋지 않습니다.
     * 대신 토큰에 userId를 포함시키는 것을 권장합니다.
     * 
     * @param token Access Token
     * @return 사용자 ID 또는 null
     */
    public String getUserIdByToken(String token) {
        // Redis에서 모든 access_token:* 키를 스캔하여 값이 일치하는 키 찾기
        // 성능상 권장하지 않지만, 필요시 사용 가능
        String pattern = TOKEN_PREFIX + "*";
        return redisTemplate.keys(pattern).stream()
                .filter(key -> token.equals(redisTemplate.opsForValue().get(key)))
                .map(key -> key.toString().replace(TOKEN_PREFIX, ""))
                .findFirst()
                .orElse(null);
    }
}
