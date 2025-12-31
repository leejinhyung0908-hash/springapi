#!/bin/bash

set -e

echo "🚀 Docker 배포를 시작합니다..."

# Docker 이미지 태그를 인자로 받기
DOCKER_IMAGE=${1:-"${DOCKERHUB_USERNAME:-your-username}/springapi:latest"}
SERVICE_NAME="springapi"
CONTAINER_NAME="springapi-container"
PROJECT_DIR="/home/ubuntu/springall/springapi"

echo "📦 Docker 이미지: $DOCKER_IMAGE"
echo "📁 프로젝트 디렉토리: $PROJECT_DIR"

# Docker 설치 확인
if ! command -v docker &> /dev/null; then
    echo "📦 Docker를 설치합니다..."
    sudo apt-get update
    sudo apt-get install -y docker.io
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker ubuntu
    echo "✅ Docker 설치 완료 (재로그인 필요할 수 있음)"
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

# 기존 컨테이너 중지 및 제거
if [ "$(sudo docker ps -aq -f name=$CONTAINER_NAME)" ]; then
    echo "⏸️  기존 컨테이너를 중지합니다..."
    sudo docker stop $CONTAINER_NAME || true
    sudo docker rm $CONTAINER_NAME || true
fi

# Docker 이미지 pull
echo "📥 Docker 이미지를 가져옵니다..."
sudo docker pull "$DOCKER_IMAGE"

# 새 컨테이너 실행
echo "▶️  새 컨테이너를 시작합니다..."
sudo docker run -d \
  --name $CONTAINER_NAME \
  --restart unless-stopped \
  -p 8080:8080 \
  --env-file "$PROJECT_DIR/.env" \
  "$DOCKER_IMAGE"

# 컨테이너 상태 확인
sleep 3
if ! sudo docker ps | grep -q $CONTAINER_NAME; then
    echo "❌ 컨테이너가 시작되지 않았습니다!"
    echo "📋 컨테이너 로그:"
    sudo docker logs $CONTAINER_NAME
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
    
    # 5번째 시도마다 컨테이너 상태 확인
    if [ $((RETRY_COUNT % 5)) -eq 0 ]; then
        if ! sudo docker ps | grep -q $CONTAINER_NAME; then
            echo "❌ 컨테이너가 중지되었습니다!"
            echo "📋 컨테이너 로그:"
            sudo docker logs $CONTAINER_NAME
            break
        fi
    fi
    
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "❌ 헬스 체크 실패!"
    echo "📋 컨테이너 상태:"
    sudo docker ps -a | grep $CONTAINER_NAME || true
    echo "📋 컨테이너 로그 (최근 50줄):"
    sudo docker logs --tail 50 $CONTAINER_NAME || true
    exit 1
fi

# 컨테이너 상태 확인
echo "📊 컨테이너 상태를 확인합니다..."
sudo docker ps | grep $CONTAINER_NAME

echo "✅ Docker 배포가 완료되었습니다!"

