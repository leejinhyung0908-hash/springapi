# 토큰 저장 확인 가이드

## 1. Upstash Redis에서 Access Token 확인

### 방법 1: Upstash Console 사용 (권장)

1. **Upstash 대시보드 접속**
   - https://console.upstash.com/ 접속
   - 로그인 후 Redis 데이터베이스 선택

2. **Redis Console 열기**
   - 왼쪽 메뉴에서 "Console" 또는 "Data Browser" 클릭
   - 또는 "Redis CLI" 탭 선택

3. **키 조회**
   ```redis
   # 모든 Access Token 키 조회
   KEYS access_token:*
   
   # 특정 사용자의 토큰 조회 (예: userId가 "123456789"인 경우)
   GET access_token:123456789
   
   # TTL 확인 (만료까지 남은 시간)
   TTL access_token:123456789
   ```

4. **예상 결과**
   ```
   > KEYS access_token:*
   1) "access_token:123456789"
   2) "access_token:987654321"
   
   > GET access_token:123456789
   "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   
   > TTL access_token:123456789
   (integer) 86400  # 24시간 (초 단위)
   ```

### 방법 2: EC2에서 Redis CLI 사용

```bash
# EC2에 SSH 접속
ssh ubuntu@your-ec2-ip

# Redis CLI 설치 (없는 경우)
sudo apt-get update
sudo apt-get install -y redis-tools

# Upstash Redis 연결 (환경 변수에서 URL 가져오기)
# .env 파일에서 UPSTASH_REDIS_URL 확인 후
redis-cli -h your-redis-endpoint.upstash.io -p 6379 -a your-redis-token

# 또는 URL에서 직접 파싱
# redis://default:TOKEN@HOST:PORT 형식에서
# HOST, PORT, TOKEN 추출하여 사용

# 키 조회
KEYS access_token:*

# 특정 키 조회
GET access_token:YOUR_USER_ID
```

### 방법 3: API 엔드포인트 추가 (개발용)

개발/디버깅용으로 API 엔드포인트를 추가할 수 있습니다:

```java
// AuthController.java에 추가 (개발 환경에서만 활성화)
@GetMapping("/debug/tokens")
public ResponseEntity<?> getTokens(@RequestParam String userId) {
    String token = accessTokenService.getToken(userId);
    return ResponseEntity.ok(Map.of(
        "userId", userId,
        "hasToken", token != null,
        "tokenLength", token != null ? token.length() : 0
    ));
}
```

## 2. Neon DB에서 Refresh Token 확인

### 방법 1: Neon Console 사용 (권장)

1. **Neon 대시보드 접속**
   - https://console.neon.tech/ 접속
   - 로그인 후 프로젝트 선택

2. **SQL Editor 열기**
   - 왼쪽 메뉴에서 "SQL Editor" 클릭
   - 또는 "Query" 탭 선택

3. **쿼리 실행**
   ```sql
   -- 모든 Refresh Token 조회
   SELECT * FROM refresh_tokens;
   
   -- 특정 사용자의 토큰 조회
   SELECT * FROM refresh_tokens WHERE "userId" = 'YOUR_USER_ID';
   
   -- 만료되지 않은 토큰만 조회
   SELECT * FROM refresh_tokens WHERE expires_at > NOW();
   
   -- 토큰 개수 확인
   SELECT COUNT(*) FROM refresh_tokens;
   
   -- 최근 생성된 토큰 조회
   SELECT * FROM refresh_tokens ORDER BY created_at DESC LIMIT 10;
   ```

4. **예상 결과**
   ```
   id | token                                    | userId    | expires_at           | created_at
   ---|------------------------------------------|-----------|----------------------|-------------------
   1  | eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... | 123456789 | 2026-01-13 12:00:00  | 2026-01-06 12:00:00
   ```

### 방법 2: psql CLI 사용

```bash
# EC2에 SSH 접속
ssh ubuntu@your-ec2-ip

# psql 설치 (없는 경우)
sudo apt-get update
sudo apt-get install -y postgresql-client

# Neon DB 연결
psql "postgresql://neondb_owner:YOUR_PASSWORD@ep-gentle-tooth-a1lvk1j6-pooler.ap-southeast-1.aws.neon.tech/neondb?sslmode=require"

# 또는 환경 변수 사용
export PGPASSWORD="YOUR_PASSWORD"
psql -h ep-gentle-tooth-a1lvk1j6-pooler.ap-southeast-1.aws.neon.tech \
     -U neondb_owner \
     -d neondb \
     -p 5432

# 쿼리 실행
SELECT * FROM refresh_tokens;
```

### 방법 3: Docker 컨테이너에서 확인

```bash
# EC2에서 실행 중인 컨테이너에 접속
sudo docker exec -it springapi-container bash

# 애플리케이션 로그 확인 (토큰 저장 로그)
# 로그에서 "Redis에 저장" 또는 "DB에 저장" 메시지 확인
```

## 3. 로그인 테스트 후 확인

### 테스트 절차

1. **프론트엔드에서 로그인**
   - 카카오/네이버/구글 로그인 실행
   - 로그인 성공 확인

2. **백엔드 로그 확인**
   ```bash
   # EC2에서
   sudo docker logs -f springapi-container | grep -i "token\|redis\|db"
   ```

3. **토큰 저장 확인**
   - Upstash Redis: `KEYS access_token:*` 실행
   - Neon DB: `SELECT * FROM refresh_tokens;` 실행

## 4. 문제 해결

### Redis에 토큰이 없는 경우

1. **Redis 연결 확인**
   ```bash
   # EC2에서 Redis 연결 테스트
   redis-cli -h your-redis-endpoint.upstash.io -p 6379 -a your-token PING
   # 응답: PONG (연결 성공)
   ```

2. **애플리케이션 로그 확인**
   ```bash
   sudo docker logs springapi-container | grep -i "redis\|error"
   ```

3. **환경 변수 확인**
   ```bash
   sudo docker exec springapi-container env | grep UPSTASH
   ```

### DB에 토큰이 없는 경우

1. **DB 연결 확인**
   ```bash
   # EC2에서 DB 연결 테스트
   psql "jdbc:postgresql://..." # 연결 테스트
   ```

2. **테이블 존재 확인**
   ```sql
   SELECT table_name FROM information_schema.tables 
   WHERE table_schema = 'public' AND table_name = 'refresh_tokens';
   ```

3. **애플리케이션 로그 확인**
   ```bash
   sudo docker logs springapi-container | grep -i "database\|jpa\|hibernate"
   ```

## 5. 보안 주의사항

⚠️ **프로덕션 환경에서는 디버그 엔드포인트를 비활성화하세요!**

- 토큰 정보는 민감한 정보입니다
- 로그에 토큰 전체를 출력하지 마세요
- 디버그용 API는 개발 환경에서만 사용하세요

