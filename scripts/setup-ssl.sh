#!/bin/bash

set -e

echo "🔒 SSL 인증서 발급을 시작합니다..."

# 도메인 이름 확인
if [ -z "$1" ]; then
    echo "❌ 사용법: ./setup-ssl.sh <도메인> <이메일>"
    echo "예시: ./setup-ssl.sh api.kroaddy.site admin@example.com"
    exit 1
fi

DOMAIN=$1
EMAIL=${2:-"admin@${DOMAIN}"}
NGINX_CONF="/etc/nginx/sites-available/$DOMAIN"

echo "📋 설정 정보:"
echo "  - 도메인: $DOMAIN"
echo "  - 이메일: $EMAIL"

# Nginx 설정 파일 확인
if [ ! -f "$NGINX_CONF" ]; then
    echo "❌ Nginx 설정 파일이 없습니다. 먼저 setup-nginx.sh를 실행하세요."
    exit 1
fi

# DNS 확인
echo "🔍 DNS 설정을 확인합니다..."
EC2_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "")
DOMAIN_IP=$(dig +short $DOMAIN | tail -1 || echo "")

if [ -n "$EC2_IP" ] && [ -n "$DOMAIN_IP" ]; then
    if [ "$EC2_IP" != "$DOMAIN_IP" ]; then
        echo "⚠️  경고: 도메인 IP($DOMAIN_IP)와 EC2 IP($EC2_IP)가 일치하지 않습니다."
        echo "   DNS 설정을 확인하세요."
        read -p "계속하시겠습니까? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        echo "✅ DNS 설정이 올바릅니다. ($DOMAIN -> $DOMAIN_IP)"
    fi
fi

# Certbot으로 SSL 인증서 발급
echo "📜 Let's Encrypt SSL 인증서를 발급받습니다..."
sudo certbot --nginx \
    -d $DOMAIN \
    --email $EMAIL \
    --agree-tos \
    --non-interactive \
    --redirect

# SSL 인증서 자동 갱신 설정
echo "🔄 SSL 인증서 자동 갱신을 설정합니다..."
if ! sudo crontab -l 2>/dev/null | grep -q "certbot renew"; then
    (sudo crontab -l 2>/dev/null; echo "0 3 * * * certbot renew --quiet --post-hook 'systemctl reload nginx'") | sudo crontab -
    echo "✅ 자동 갱신 cron 작업이 추가되었습니다."
else
    echo "✅ 자동 갱신 cron 작업이 이미 설정되어 있습니다."
fi

# Nginx 설정 확인
echo "🧪 Nginx 설정을 테스트합니다..."
sudo nginx -t

# Nginx 재시작
echo "🔄 Nginx를 재시작합니다..."
sudo systemctl reload nginx

# 인증서 정보 확인
echo ""
echo "✅ SSL 인증서 발급이 완료되었습니다!"
echo ""
echo "📋 인증서 정보:"
sudo certbot certificates

echo ""
echo "🌐 HTTPS 테스트:"
echo "  curl -I https://$DOMAIN"
echo ""
echo "📝 다음 단계:"
echo "  1. Spring Boot 애플리케이션의 환경 변수를 업데이트하세요:"
echo "     - COOKIE_SECURE=true"
echo "     - KAKAO_REDIRECT_URI=https://$DOMAIN/api/auth/kakao/callback"
echo "     - NAVER_REDIRECT_URI=https://$DOMAIN/api/auth/naver/callback"
echo "     - GOOGLE_REDIRECT_URI=https://$DOMAIN/api/auth/google/callback"
echo "     - FRONT_LOGIN_CALLBACK_URL=https://your-frontend-domain.com"
echo ""
echo "  2. 애플리케이션을 재배포하세요."

