# Nginx + HTTPS 설정 가이드

EC2에 Nginx를 설치하고 Let's Encrypt SSL 인증서를 발급받아 HTTPS를 적용하는 방법입니다.

## 사전 준비

1. **도메인 DNS 설정**
   - 도메인의 A 레코드가 EC2 인스턴스의 공인 IP를 가리키도록 설정
   - DNS 전파 확인: `dig your-domain.com` 또는 `nslookup your-domain.com`

2. **EC2 보안 그룹 설정**
   - 인바운드 규칙에 HTTP(80), HTTPS(443) 포트 허용

## 설치 방법

### 방법 1: 통합 스크립트 사용 (권장)

```bash
# EC2에 SSH 접속
ssh -i your-key.pem ubuntu@your-ec2-ip

# 프로젝트 디렉토리로 이동
cd /home/ubuntu/springall/springapi

# 최신 코드 가져오기
git pull origin main

# 스크립트 실행 권한 부여
chmod +x scripts/setup-nginx-https.sh

# 통합 설치 실행
./scripts/setup-nginx-https.sh api.kroaddy.site admin@example.com
```

### 방법 2: 단계별 실행

#### 1단계: Nginx 설치 및 기본 설정

```bash
chmod +x scripts/setup-nginx.sh
./scripts/setup-nginx.sh api.kroaddy.site admin@example.com
```

#### 2단계: SSL 인증서 발급

```bash
chmod +x scripts/setup-ssl.sh
./scripts/setup-ssl.sh api.kroaddy.site admin@example.com
```

## 설정 확인

### Nginx 상태 확인

```bash
# Nginx 상태
sudo systemctl status nginx

# Nginx 설정 테스트
sudo nginx -t

# Nginx 재시작
sudo systemctl restart nginx
```

### SSL 인증서 확인

```bash
# 인증서 정보 확인
sudo certbot certificates

# 인증서 만료일 확인
sudo certbot certificates | grep Expiry
```

### HTTPS 테스트

```bash
# HTTP -> HTTPS 리다이렉트 확인
curl -I http://api.kroaddy.site

# HTTPS 연결 확인
curl -I https://api.kroaddy.site

# 헬스 체크
curl https://api.kroaddy.site/actuator/health
```

## 자동 갱신

SSL 인증서는 자동으로 갱신되도록 설정됩니다. 수동으로 갱신하려면:

```bash
# 인증서 갱신 테스트
sudo certbot renew --dry-run

# 실제 갱신
sudo certbot renew
```

## Spring Boot 환경 변수 업데이트

HTTPS 적용 후 Spring Boot 애플리케이션의 환경 변수를 업데이트하세요:

```bash
# .env 파일 편집
nano /home/ubuntu/springall/springapi/.env
```

다음 변수들을 업데이트:

```env
# Cookie 보안 설정
COOKIE_SECURE=true
COOKIE_SAME_SITE=Lax

# OAuth 리다이렉트 URI (HTTPS로 변경)
KAKAO_REDIRECT_URI=https://api.kroaddy.site/api/auth/kakao/callback
NAVER_REDIRECT_URI=https://api.kroaddy.site/api/auth/naver/callback
GOOGLE_REDIRECT_URI=https://api.kroaddy.site/api/auth/google/callback

# 프론트엔드 URL
FRONT_LOGIN_CALLBACK_URL=https://your-frontend-domain.com
```

환경 변수 업데이트 후 컨테이너 재시작:

```bash
sudo docker restart springapi-container
```

## Nginx 로그 확인

```bash
# 액세스 로그
sudo tail -f /var/log/nginx/springapi-access.log

# 에러 로그
sudo tail -f /var/log/nginx/springapi-error.log

# 전체 로그
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

## 문제 해결

### 1. DNS 전파 확인

```bash
# EC2 공인 IP 확인
curl http://169.254.169.254/latest/meta-data/public-ipv4

# 도메인 IP 확인
dig +short api.kroaddy.site

# 두 IP가 일치해야 함
```

### 2. 포트 확인

```bash
# 80, 443 포트 리스닝 확인
sudo ss -tlnp | grep -E ':(80|443)'

# 방화벽 확인
sudo ufw status
```

### 3. Nginx 설정 확인

```bash
# 설정 파일 확인
sudo nginx -t

# 활성화된 설정 확인
ls -la /etc/nginx/sites-enabled/

# 설정 파일 내용 확인
sudo cat /etc/nginx/sites-available/api.kroaddy.site
```

### 4. Certbot 에러

```bash
# Certbot 로그 확인
sudo tail -f /var/log/letsencrypt/letsencrypt.log

# 인증서 발급 재시도
sudo certbot --nginx -d api.kroaddy.site --force-renewal
```

## 보안 권장 사항

1. **방화벽 설정**: UFW를 사용하여 필요한 포트만 열기
2. **Nginx 업데이트**: 정기적으로 Nginx 업데이트
3. **SSL 설정**: 강력한 암호화 프로토콜 사용 (TLS 1.2 이상)
4. **보안 헤더**: HSTS, X-Frame-Options 등 설정
5. **Rate Limiting**: DDoS 공격 방지를 위한 제한 설정

## 참고 자료

- [Nginx 공식 문서](https://nginx.org/en/docs/)
- [Let's Encrypt 문서](https://letsencrypt.org/docs/)
- [Certbot 문서](https://certbot.eff.org/)

