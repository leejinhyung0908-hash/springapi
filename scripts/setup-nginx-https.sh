#!/bin/bash

# Nginx + HTTPS 통합 설정 스크립트
# 사용법: ./setup-nginx-https.sh <도메인> <이메일>

set -e

if [ -z "$1" ]; then
    echo "❌ 사용법: ./setup-nginx-https.sh <도메인> <이메일>"
    echo "예시: ./setup-nginx-https.sh api.kroaddy.site admin@example.com"
    exit 1
fi

DOMAIN=$1
EMAIL=${2:-"admin@${DOMAIN}"}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Nginx + HTTPS 설정을 시작합니다..."
echo "  도메인: $DOMAIN"
echo "  이메일: $EMAIL"
echo ""

# 1. Nginx 설치 및 기본 설정
echo "📦 1단계: Nginx 설치 및 기본 설정"
bash "$SCRIPT_DIR/setup-nginx.sh" "$DOMAIN" "$EMAIL"

# 잠시 대기 (DNS 전파 대기)
echo ""
echo "⏳ DNS 전파를 위해 10초 대기합니다..."
sleep 10

# 2. SSL 인증서 발급
echo ""
echo "🔒 2단계: SSL 인증서 발급"
bash "$SCRIPT_DIR/setup-ssl.sh" "$DOMAIN" "$EMAIL"

echo ""
echo "✅ 모든 설정이 완료되었습니다!"
echo ""
echo "🌐 테스트:"
echo "  curl -I https://$DOMAIN"
echo "  curl https://$DOMAIN/actuator/health"
echo ""

