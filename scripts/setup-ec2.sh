#!/bin/bash

set -e

echo "🚀 EC2 초기 설정을 시작합니다..."

# Java 21 설치 확인 및 설치
if ! command -v java &> /dev/null || ! java -version 2>&1 | grep -q "21"; then
    echo "📦 Java 21을 설치합니다..."
    sudo apt-get update
    sudo apt-get install -y openjdk-21-jdk
    echo "✅ Java 21 설치 완료"
else
    echo "✅ Java 21이 이미 설치되어 있습니다."
fi

# 프로젝트 디렉토리 생성
PROJECT_DIR="/home/ubuntu/springapi"
if [ ! -d "$PROJECT_DIR" ]; then
    mkdir -p "$PROJECT_DIR"
    echo "✅ 프로젝트 디렉토리 생성: $PROJECT_DIR"
fi

# 로그 디렉토리 생성
LOG_DIR="/var/log/springapi"
if [ ! -d "$LOG_DIR" ]; then
    sudo mkdir -p "$LOG_DIR"
    sudo chown ubuntu:ubuntu "$LOG_DIR"
    echo "✅ 로그 디렉토리 생성: $LOG_DIR"
fi

# 백업 디렉토리 생성
BACKUP_DIR="/home/ubuntu/springapi/backups"
if [ ! -d "$BACKUP_DIR" ]; then
    mkdir -p "$BACKUP_DIR"
    echo "✅ 백업 디렉토리 생성: $BACKUP_DIR"
fi

echo "✅ EC2 초기 설정이 완료되었습니다!"

