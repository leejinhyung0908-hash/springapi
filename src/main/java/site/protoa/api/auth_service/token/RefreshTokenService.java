package site.protoa.api.auth_service.token;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token을 Neon DB에 저장/조회/삭제하는 서비스
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Refresh Token을 DB에 저장
     * 기존 토큰이 있으면 업데이트, 없으면 새로 생성
     * 
     * @param userId 사용자 ID
     * @param token Refresh Token
     * @param expirationSeconds 만료 시간 (초)
     */
    @Transactional
    public void saveToken(String userId, String token, long expirationSeconds) {
        // 기존 토큰이 있으면 삭제
        refreshTokenRepository.findByUserId(userId).ifPresent(existing -> {
            refreshTokenRepository.delete(existing);
        });

        // 새 토큰 저장
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setToken(token);
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(expirationSeconds));
        refreshTokenRepository.save(entity);
    }

    /**
     * Refresh Token을 DB에서 조회
     * 
     * @param token Refresh Token
     * @return RefreshTokenEntity 또는 null
     */
    public Optional<RefreshTokenEntity> getToken(String token) {
        Optional<RefreshTokenEntity> entity = refreshTokenRepository.findByToken(token);
        
        // 만료 확인
        if (entity.isPresent() && entity.get().getExpiresAt().isBefore(LocalDateTime.now())) {
            // 만료된 토큰 삭제
            refreshTokenRepository.delete(entity.get());
            return Optional.empty();
        }
        
        return entity;
    }

    /**
     * 사용자 ID로 Refresh Token 조회
     * 
     * @param userId 사용자 ID
     * @return RefreshTokenEntity 또는 null
     */
    public Optional<RefreshTokenEntity> getTokenByUserId(String userId) {
        Optional<RefreshTokenEntity> entity = refreshTokenRepository.findByUserId(userId);
        
        // 만료 확인
        if (entity.isPresent() && entity.get().getExpiresAt().isBefore(LocalDateTime.now())) {
            // 만료된 토큰 삭제
            refreshTokenRepository.delete(entity.get());
            return Optional.empty();
        }
        
        return entity;
    }

    /**
     * Refresh Token을 DB에서 삭제
     * 
     * @param token Refresh Token
     */
    @Transactional
    public void deleteToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    /**
     * 사용자 ID로 Refresh Token 삭제
     * 
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteTokenByUserId(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * Refresh Token 존재 여부 확인
     * 
     * @param token Refresh Token
     * @return 존재 여부
     */
    public boolean existsToken(String token) {
        Optional<RefreshTokenEntity> entity = refreshTokenRepository.findByToken(token);
        if (entity.isPresent()) {
            // 만료 확인
            if (entity.get().getExpiresAt().isBefore(LocalDateTime.now())) {
                refreshTokenRepository.delete(entity.get());
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 만료된 토큰 정리 (스케줄러에서 주기적으로 호출)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}

