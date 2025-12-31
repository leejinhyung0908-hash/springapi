# 1단계: 빌드
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew build -x test

# 2단계: 실행
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# JAR 파일 복사 (모든 JAR 파일을 복사하고 나중에 선택)
COPY --from=builder /app/build/libs/ ./libs/
RUN find ./libs -name "*.jar" ! -name "*-plain.jar" -exec mv {} app.jar \;

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx1024m", "app.jar"]

