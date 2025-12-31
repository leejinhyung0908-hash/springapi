#!/bin/bash

set -e

echo "🚀 배포를 시작합니다..."

# 현재 스크립트가 있는 디렉토리 기준으로 프로젝트 루트 찾기
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SERVICE_NAME="springapi"
BACKUP_DIR="$PROJECT_DIR/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo "📁 프로젝트 디렉토리: $PROJECT_DIR"

# 이전 버전 백업
if [ -f "$PROJECT_DIR/app.jar" ]; then
    echo "📦 이전 버전을 백업합니다..."
    mkdir -p "$BACKUP_DIR"
    cp "$PROJECT_DIR/app.jar" "$BACKUP_DIR/app.jar.$TIMESTAMP"
    echo "✅ 백업 완료: $BACKUP_DIR/app.jar.$TIMESTAMP"
fi

# .env 파일 확인
if [ ! -f "$PROJECT_DIR/.env" ]; then
    if [ -f "$PROJECT_DIR/.env.example" ]; then
        echo "⚠️  .env 파일이 없습니다. .env.example을 복사합니다."
        cp "$PROJECT_DIR/.env.example" "$PROJECT_DIR/.env"
        echo "📝 $PROJECT_DIR/.env 파일을 편집해주세요."
    else
        echo "⚠️  .env 파일이 없습니다. 환경 변수를 설정해주세요."
    fi
fi

# 서비스 중지 (실행 중인 경우)
if systemctl is-active --quiet "${SERVICE_NAME}.service"; then
    echo "⏸️  서비스를 중지합니다..."
    sudo systemctl stop "${SERVICE_NAME}.service"
    sleep 2
fi

# 새 버전으로 교체
if [ -f "$PROJECT_DIR/app.jar" ]; then
    echo "🔄 새 버전으로 교체합니다..."
    # 새 JAR 파일이 이미 배포되어 있다고 가정
    # (GitHub Actions에서 이미 복사됨)
fi

# 서비스 시작
echo "▶️  서비스를 시작합니다..."
sudo systemctl start "${SERVICE_NAME}.service"
sleep 5

# 서비스 상태 확인
echo "📊 서비스 상태 확인..."
if ! systemctl is-active --quiet "${SERVICE_NAME}.service"; then
    echo "❌ 서비스가 시작되지 않았습니다!"
    echo "📋 서비스 로그:"
    sudo journalctl -u "${SERVICE_NAME}.service" -n 50 --no-pager
    exit 1
fi

# 헬스 체크
echo "🏥 헬스 체크를 수행합니다..."
MAX_RETRIES=30
RETRY_COUNT=0
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f -s "$HEALTH_CHECK_URL" > /dev/null 2>&1; then
        echo "✅ 헬스 체크 성공!"
        break
    fi
    
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "⏳ 헬스 체크 대기 중... ($RETRY_COUNT/$MAX_RETRIES)"
    
    # 5번째 시도마다 서비스 상태 확인
    if [ $((RETRY_COUNT % 5)) -eq 0 ]; then
        if ! systemctl is-active --quiet "${SERVICE_NAME}.service"; then
            echo "❌ 서비스가 중지되었습니다!"
            echo "📋 서비스 로그:"
            sudo journalctl -u "${SERVICE_NAME}.service" -n 50 --no-pager
            break
        fi
    fi
    
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "❌ 헬스 체크 실패!"
    echo "📋 서비스 상태:"
    sudo systemctl status "${SERVICE_NAME}.service" --no-pager -l || true
    echo "📋 서비스 로그 (최근 50줄):"
    sudo journalctl -u "${SERVICE_NAME}.service" -n 50 --no-pager || true
    echo "📋 포트 확인:"
    sudo netstat -tlnp | grep 8080 || ss -tlnp | grep 8080 || true
    
    # 롤백
    if [ -f "$BACKUP_DIR/app.jar.$TIMESTAMP" ]; then
        echo "🔄 이전 버전으로 롤백합니다..."
        cp "$BACKUP_DIR/app.jar.$TIMESTAMP" "$PROJECT_DIR/app.jar"
        sudo systemctl restart "${SERVICE_NAME}.service"
        echo "✅ 롤백 완료"
    else
        echo "❌ 롤백할 백업 파일이 없습니다."
    fi
    
    exit 1
fi

# 서비스 상태 확인
echo "📊 서비스 상태를 확인합니다..."
sudo systemctl status "${SERVICE_NAME}.service" --no-pager -l

echo "✅ 배포가 완료되었습니다!"

