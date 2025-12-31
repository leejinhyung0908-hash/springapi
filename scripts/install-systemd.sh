#!/bin/bash

set -e

echo "🔧 systemd 서비스를 설치합니다..."

PROJECT_DIR="/home/ubuntu/springapi"
SERVICE_NAME="springapi"
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

# systemd 서비스 파일 복사
if [ -f "$PROJECT_DIR/scripts/${SERVICE_NAME}.service" ]; then
    sudo cp "$PROJECT_DIR/scripts/${SERVICE_NAME}.service" "$SERVICE_FILE"
    echo "✅ systemd 서비스 파일 복사 완료"
else
    echo "❌ 서비스 파일을 찾을 수 없습니다: $PROJECT_DIR/scripts/${SERVICE_NAME}.service"
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

