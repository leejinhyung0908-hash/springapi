#!/bin/bash

set -e

echo "🔧 systemd 서비스를 설치합니다..."

# 현재 스크립트가 있는 디렉토리 기준으로 프로젝트 루트 찾기
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SERVICE_NAME="springapi"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

echo "📁 프로젝트 디렉토리: $PROJECT_DIR"

# systemd 서비스 파일 복사 및 경로 치환
if [ -f "$SCRIPT_DIR/${SERVICE_NAME}.service" ]; then
    # 서비스 파일을 임시로 복사하고 경로를 실제 프로젝트 경로로 치환
    sed "s|%h/springall/springapi|$PROJECT_DIR|g" "$SCRIPT_DIR/${SERVICE_NAME}.service" | \
    sed "s|/home/ubuntu/springapi|$PROJECT_DIR|g" | \
    sudo tee "$SERVICE_FILE" > /dev/null
    echo "✅ systemd 서비스 파일 복사 완료 (경로: $PROJECT_DIR)"
else
    echo "❌ 서비스 파일을 찾을 수 없습니다: $SCRIPT_DIR/${SERVICE_NAME}.service"
    exit 1
fi

# systemd 데몬 리로드
sudo systemctl daemon-reload
echo "✅ systemd 데몬 리로드 완료"

# 서비스 활성화
sudo systemctl enable "${SERVICE_NAME}.service"
echo "✅ 서비스 활성화 완료"

echo "✅ systemd 서비스 설치가 완료되었습니다!"
echo "📝 서비스 시작: sudo systemctl start ${SERVICE_NAME}"
echo "📝 서비스 상태 확인: sudo systemctl status ${SERVICE_NAME}"
echo "📝 서비스 로그 확인: sudo journalctl -u ${SERVICE_NAME} -f"

