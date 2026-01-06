package site.protoa.api.auth_service.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token을 Neon DB에 저장/조회하는 Repository
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    /**
     * 토큰으로 Refresh Token 조회
     * 
     * @param token Refresh Token
     * @return RefreshTokenEntity
     */
    Optional<RefreshTokenEntity> findByToken(String token);

    /**
     * 사용자 ID로 Refresh Token 조회
     * 
     * @param userId 사용자 ID
     * @return RefreshTokenEntity
     */
    Optional<RefreshTokenEntity> findByUserId(String userId);

    /**
     * 토큰 삭제
     * 
     * @param token Refresh Token
     */
    void deleteByToken(String token);

    /**
     * 사용자 ID로 토큰 삭제
     * 
     * @param userId 사용자 ID
     */
    void deleteByUserId(String userId);

    /**
     * 만료된 토큰 삭제
     * 
     * @param now 현재 시간
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
