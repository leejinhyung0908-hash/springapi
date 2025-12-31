# Gateway 통합 전략

## 현재 상황 분석

### 1. Python Gateway (`ai.kroaddy.site/gateway/`)
- **기술 스택**: FastAPI (Python)
- **포트**: 9000
- **주요 기능**:
  - Feed 서비스 프록시 (`/feed/**`)
  - RAG 서비스 프록시 (`/rag/**`)
  - Chatbot 서비스 프록시 (`/chatbot/**`)
  - Agent 서비스 (내부 서비스, `/agent/**`)
    - LLM API 통합 (OpenAI, Anthropic)
    - SLLM DB (캐시 관리)
- **특징**: Python 서비스들과 직접 통합

### 2. Java Gateway (`api.kroaddy.site/gateway/`)
- **기술 스택**: Spring Cloud Gateway (Java 21, Spring Boot 3.5.7)
- **포트**: 8080
- **주요 기능**:
  - Auth 서비스 라우팅 (`/api/auth/**`)
  - Log 서비스 라우팅 (`/api/log/**`)
  - ML 서비스 라우팅 (`/api/ml/**`)
  - CORS 설정
  - Redis 연동 (Rate Limiting)
  - Circuit Breaker (Resilience4j)
  - Swagger/OpenAPI 문서화
- **특징**: 엔터프라이즈급 기능 (Service Discovery, Circuit Breaker 등)

---

## 통합 전략 옵션

### 옵션 1: Java Gateway를 메인으로 통합 (권장) ⭐

**장점**:
- 엔터프라이즈급 기능 (Circuit Breaker, Rate Limiting, Service Discovery)
- Spring Cloud 생태계 활용
- 성능 최적화 (WebFlux 비동기 처리)
- Swagger/OpenAPI 자동 문서화
- 프로덕션 환경에 적합

**단점**:
- Python Agent 서비스 통합 필요
- Python 서비스 프록시 추가 작업 필요

**구현 계획**:

#### 1단계: Java Gateway에 Python 서비스 라우팅 추가

```yaml
# application.yaml에 추가
spring:
  cloud:
    gateway:
      routes:
        # Feed 서비스 (Python)
        - id: feed-service
          uri: http://feedservice:9003
          predicates:
            - Path=/api/feed/**
          filters:
            - StripPrefix=2  # /api/feed 제거
        
        # RAG 서비스 (Python)
        - id: rag-service
          uri: http://ragservice:9002
          predicates:
            - Path=/api/rag/**
          filters:
            - StripPrefix=2  # /api/rag 제거
        
        # Chatbot 서비스 (Python)
        - id: chatbot-service
          uri: http://chatbotservice:9004
          predicates:
            - Path=/api/chatbot/**
          filters:
            - StripPrefix=2  # /api/chatbot 제거
        
        # Agent 서비스 (Python) - 별도 처리 필요
        - id: agent-service
          uri: http://gateway-service:9000  # Python Gateway를 내부 서비스로 유지
          predicates:
            - Path=/api/agent/**
          filters:
            - StripPrefix=2
```

#### 2단계: Agent 서비스 처리 방법

**방법 A: Python Gateway를 내부 서비스로 유지**
- Python Gateway를 별도 서비스로 유지 (포트 9000)
- Java Gateway에서 `/api/agent/**` 요청을 Python Gateway로 프록시
- 장점: Agent 서비스 코드 변경 최소화
- 단점: Gateway가 2개 존재 (하지만 역할 분리)

**방법 B: Agent 서비스를 독립 서비스로 분리**
- `ai.kroaddy.site/gateway/app/agent/`를 독립 서비스로 분리
- `ai.kroaddy.site/services/agentservice/`로 이동
- Java Gateway에서 직접 라우팅
- 장점: 완전한 단일 Gateway
- 단점: Agent 서비스 분리 작업 필요

#### 3단계: CORS 설정 통합

```java
// CorsConfig.java 수정
corsConfig.setAllowedOriginPatterns(Arrays.asList(
    "http://localhost:3000",
    "http://localhost:3001",
    "http://localhost:4000",
    "*"  // 개발 환경용 (프로덕션에서는 제거)
));
```

#### 4단계: Python Gateway 제거 또는 축소

- `ai.kroaddy.site/gateway/` 폴더 제거
- 또는 Agent 서비스만 남기고 나머지 제거

---

### 옵션 2: Python Gateway를 메인으로 통합

**장점**:
- Python 서비스들과의 통합 용이
- Agent 서비스 유지 용이
- 빠른 개발 및 수정

**단점**:
- 엔터프라이즈 기능 부족 (Circuit Breaker, Rate Limiting 등)
- Java 서비스 통합 필요
- 프로덕션 환경 대비 부족

**구현 계획**:

#### 1단계: Python Gateway에 Java 서비스 프록시 추가

```python
# main.py에 추가
AUTH_SERVICE_URL = os.getenv("AUTH_SERVICE_URL", "http://auth-service:8081")
ML_SERVICE_URL = os.getenv("ML_SERVICE_URL", "http://ml-service:9010")

# Auth 서비스 프록시
auth_router = APIRouter()

@auth_router.api_route("/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"])
async def proxy_auth(request: Request, path: str):
    # ... 프록시 로직

# ML 서비스 프록시
ml_router = APIRouter()

@ml_router.api_route("/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"])
async def proxy_ml(request: Request, path: str):
    # ... 프록시 로직

app.include_router(auth_router, prefix="/api/auth", tags=["auth"])
app.include_router(ml_router, prefix="/api/ml", tags=["ml"])
```

#### 2단계: Java Gateway 제거
- `api.kroaddy.site/gateway/` 폴더 제거 또는 비활성화

---

## 권장 전략: 옵션 1 (Java Gateway 메인)

### 이유
1. **확장성**: Spring Cloud Gateway는 대규모 마이크로서비스에 적합
2. **안정성**: Circuit Breaker, Rate Limiting 등 프로덕션 필수 기능
3. **표준화**: 엔터프라이즈 환경에서 널리 사용되는 기술 스택
4. **성능**: WebFlux 기반 비동기 처리로 높은 처리량

### 단계별 마이그레이션 계획

#### Phase 1: 준비 단계 (1주)
- [ ] Java Gateway에 Python 서비스 라우팅 추가
- [ ] CORS 설정 통합
- [ ] 환경 변수 설정 (Docker Compose)

#### Phase 2: Agent 서비스 처리 (1주)
- [ ] 방법 선택 (A 또는 B)
- [ ] Agent 서비스 통합
- [ ] 테스트

#### Phase 3: 마이그레이션 (1주)
- [ ] 프론트엔드 API 경로 변경 (`/feed/**` → `/api/feed/**`)
- [ ] Python Gateway 제거 또는 축소
- [ ] 통합 테스트

#### Phase 4: 최적화 (1주)
- [ ] Circuit Breaker 설정
- [ ] Rate Limiting 설정
- [ ] 모니터링 설정
- [ ] 문서화

---

## 구현 상세: Java Gateway 통합

### 1. application.yaml 수정

```yaml
spring:
  application:
    name: gateway
  server:
    port: 8080
  cloud:
    gateway:
      routes:
        # === Java 서비스 ===
        - id: auth-service
          uri: http://auth-service:8081
          predicates:
            - Path=/api/auth/**
        
        - id: log-service
          uri: http://auth-service:8081
          predicates:
            - Path=/api/log/**
        
        - id: ml-service
          uri: http://ml-service:9010
          predicates:
            - Path=/api/ml/**
          filters:
            - StripPrefix=2
        
        # === Python 서비스 ===
        - id: feed-service
          uri: http://feedservice:9003
          predicates:
            - Path=/api/feed/**
          filters:
            - StripPrefix=2
        
        - id: rag-service
          uri: http://ragservice:9002
          predicates:
            - Path=/api/rag/**
          filters:
            - StripPrefix=2
        
        - id: chatbot-service
          uri: http://chatbotservice:9004
          predicates:
            - Path=/api/chatbot/**
          filters:
            - StripPrefix=2
        
        # === Agent 서비스 (임시: Python Gateway로 프록시) ===
        - id: agent-service
          uri: http://gateway-python:9000
          predicates:
            - Path=/api/agent/**
          filters:
            - StripPrefix=2
        
        # === 루트 리다이렉트 ===
        - id: root-redirect
          uri: http://localhost:8080
          predicates:
            - Path=/
          filters:
            - RedirectTo=302, /docs
```

### 2. Docker Compose 수정

```yaml
services:
  # Java Gateway (메인)
  gateway:
    build: ./api.kroaddy.site/gateway
    ports:
      - "8080:8080"
    environment:
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    depends_on:
      - redis
      - auth-service
      - ml-service
      - feedservice
      - ragservice
      - chatbotservice
      - gateway-python  # Agent 서비스용
  
  # Python Gateway (Agent 서비스 전용)
  gateway-python:
    build: ./ai.kroaddy.site/gateway
    ports:
      - "9000:9000"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    # Feed, RAG, Chatbot 프록시 제거 (Java Gateway로 이동)
```

### 3. Python Gateway 축소 (Agent만 남김)

```python
# ai.kroaddy.site/gateway/app/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.agent.main import agent_router

app = FastAPI(title="Agent Service", version="1.0.0")

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Agent 라우터만 포함
app.include_router(agent_router)

@app.get("/")
async def root():
    return {
        "service": "Agent Service",
        "status": "running",
        "version": "1.0.0"
    }
```

---

## API 경로 변경 사항

### Before (Python Gateway)
- `http://localhost:9000/feed/**`
- `http://localhost:9000/rag/**`
- `http://localhost:9000/chatbot/**`
- `http://localhost:9000/agent/**`

### After (Java Gateway)
- `http://localhost:8080/api/feed/**`
- `http://localhost:8080/api/rag/**`
- `http://localhost:8080/api/chatbot/**`
- `http://localhost:8080/api/agent/**`
- `http://localhost:8080/api/auth/**`
- `http://localhost:8080/api/ml/**`
- `http://localhost:8080/api/log/**`

---

## 롤백 계획

문제 발생 시:
1. Java Gateway 라우팅 비활성화
2. Python Gateway로 복귀
3. 프론트엔드 API 경로 원복

---

## 체크리스트

### Phase 1: 준비
- [ ] Java Gateway에 Python 서비스 라우팅 추가
- [ ] CORS 설정 확인
- [ ] Docker Compose 환경 변수 설정
- [ ] 로컬 테스트

### Phase 2: Agent 서비스
- [ ] Agent 서비스 통합 방법 결정
- [ ] Python Gateway 축소 또는 Agent 서비스 분리
- [ ] Agent API 테스트

### Phase 3: 마이그레이션
- [ ] 프론트엔드 API 경로 변경
- [ ] 통합 테스트
- [ ] Python Gateway 제거 또는 축소

### Phase 4: 최적화
- [ ] Circuit Breaker 설정
- [ ] Rate Limiting 설정
- [ ] 모니터링 설정
- [ ] 문서 업데이트

---

## 참고사항

1. **포트 변경**: Gateway 포트가 9000 → 8080으로 변경됨
2. **API 경로 통일**: 모든 API가 `/api/**` 경로로 통일됨
3. **환경 변수**: Docker Compose에서 서비스 URL 설정 필요
4. **문서화**: Swagger UI는 `http://localhost:8080/docs`에서 확인 가능

---

**작성일**: 2025년 1월  
**작성자**: AI Assistant

