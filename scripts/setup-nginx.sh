#!/bin/bash

set -e

echo "🔧 Nginx 설치 및 설정을 시작합니다..."

# 도메인 이름 확인
if [ -z "$1" ]; then
    echo "❌ 사용법: ./setup-nginx.sh <도메인> <이메일>"
    echo "예시: ./setup-nginx.sh api.kroaddy.site admin@example.com"
    exit 1
fi

DOMAIN=$1
EMAIL=${2:-"admin@${DOMAIN}"}
PROJECT_DIR="/home/ubuntu/springall/springapi"
NGINX_CONF_DIR="/etc/nginx/sites-available"
NGINX_ENABLED_DIR="/etc/nginx/sites-enabled"

echo "📋 설정 정보:"
echo "  - 도메인: $DOMAIN"
echo "  - 이메일: $EMAIL"
echo "  - 프로젝트 디렉토리: $PROJECT_DIR"

# Nginx 설치
if ! command -v nginx &> /dev/null; then
    echo "📦 Nginx를 설치합니다..."
    sudo apt-get update
    sudo apt-get install -y nginx
    sudo systemctl enable nginx
    echo "✅ Nginx 설치 완료"
else
    echo "✅ Nginx가 이미 설치되어 있습니다."
fi

# Certbot 설치
if ! command -v certbot &> /dev/null; then
    echo "📦 Certbot을 설치합니다..."
    sudo apt-get install -y certbot python3-certbot-nginx
    echo "✅ Certbot 설치 완료"
else
    echo "✅ Certbot이 이미 설치되어 있습니다."
fi

# 기본 Nginx 설정 제거
if [ -f "$NGINX_ENABLED_DIR/default" ]; then
    echo "🗑️  기본 Nginx 설정을 제거합니다..."
    sudo rm -f "$NGINX_ENABLED_DIR/default"
fi

# Nginx 설정 파일 생성
echo "📝 Nginx 설정 파일을 생성합니다..."
sudo tee "$NGINX_CONF_DIR/$DOMAIN" > /dev/null <<EOF
# HTTP 서버 (SSL 인증서 발급 전 임시)
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN;

    # Let's Encrypt 인증서 발급을 위한 경로
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }

    # 임시로 Spring Boot로 프록시
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        
        # WebSocket 지원
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
EOF

# 설정 파일 활성화
echo "🔗 Nginx 설정을 활성화합니다..."
sudo ln -sf "$NGINX_CONF_DIR/$DOMAIN" "$NGINX_ENABLED_DIR/$DOMAIN"

# Nginx 설정 테스트
echo "🧪 Nginx 설정을 테스트합니다..."
sudo nginx -t

# Nginx 재시작
echo "🔄 Nginx를 재시작합니다..."
sudo systemctl restart nginx

# 방화벽 설정 (UFW 사용 시)
if command -v ufw &> /dev/null; then
    echo "🔥 방화벽 규칙을 설정합니다..."
    sudo ufw allow 'Nginx Full' 2>/dev/null || true
    sudo ufw allow 80/tcp 2>/dev/null || true
    sudo ufw allow 443/tcp 2>/dev/null || true
    echo "✅ 방화벽 규칙 설정 완료"
fi

echo ""
echo "✅ Nginx 기본 설정이 완료되었습니다!"
echo ""
echo "📋 다음 단계:"
echo "  1. 도메인의 DNS A 레코드가 이 EC2 인스턴스의 IP를 가리키는지 확인하세요."
echo "  2. 다음 명령어로 SSL 인증서를 발급받으세요:"
echo "     sudo certbot --nginx -d $DOMAIN --email $EMAIL --agree-tos --non-interactive"
echo ""
echo "   또는 자동 설정 스크립트를 실행하세요:"
echo "     ./scripts/setup-ssl.sh $DOMAIN $EMAIL"

