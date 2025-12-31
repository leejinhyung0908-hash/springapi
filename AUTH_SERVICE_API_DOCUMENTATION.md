# Auth Service API 문서

## 개요

`api.kroaddy.site`의 `auth_service`는 소셜 로그인(카카오, 네이버, 구글)과 JWT 기반 인증을 제공하는 서비스입니다.

## API 엔드포인트

### 1. 인증 상태 확인

**엔드포인트:** `GET /api/auth/me`

**설명:** 쿠키에서 JWT 토큰을 읽어 검증하고, 사용자 정보를 반환합니다.

**요청:**
- 쿠키: `Authorization` (JWT 토큰)

**응답:**
```json
{
  "id": "user-id"
}
```

**에러 응답:**
```json
{
  "error": "Unauthorized",
  "message": "인증이 필요합니다."
}
```

**상태 코드:**
- `200 OK`: 성공
- `401 Unauthorized`: 인증 실패
- `500 Internal Server Error`: 서버 오류

---

### 2. 토큰 갱신

**엔드포인트:** `POST /api/auth/refresh`

**설명:** Refresh Token으로 새로운 Access Token을 발급합니다.

**요청:**
- 쿠키: `RefreshToken` (Refresh Token)

**응답:**
```json
{
  "success": true,
  "message": "토큰이 갱신되었습니다."
}
```

**에러 응답:**
```json
{
  "error": "Unauthorized",
  "message": "Refresh Token이 필요합니다."
}
```

**상태 코드:**
- `200 OK`: 성공
- `401 Unauthorized`: Refresh Token 없음 또는 유효하지 않음
- `500 Internal Server Error`: 서버 오류

---

### 3. 로그아웃

**엔드포인트:** `POST /api/auth/logout`

**설명:** 쿠키에서 Access Token과 Refresh Token을 삭제합니다.

**요청:**
- 쿠키: `Authorization`, `RefreshToken`

**응답:**
```json
{
  "success": true,
  "message": "로그아웃되었습니다."
}
```

**에러 응답:**
```json
{
  "success": false,
  "error": "로그아웃 처리 중 오류가 발생했습니다: ..."
}
```

**상태 코드:**
- `200 OK`: 성공
- `500 Internal Server Error`: 서버 오류

---

### 4. 카카오 로그인

#### 4.1 로그인 URL 가져오기

**엔드포인트:** `GET /api/auth/kakao/login`

**설명:** 카카오 인가 URL을 생성하여 반환합니다.

**요청:**
- 헤더 (선택): `X-Frontend-Callback-Url` - 프론트엔드 콜백 URL

**응답:**
```json
{
  "authUrl": "https://kauth.kakao.com/oauth/authorize?..."
}
```

**상태 코드:**
- `200 OK`: 성공

#### 4.2 카카오 콜백 처리

**엔드포인트:** `GET /api/auth/kakao/callback`

**설명:** 카카오 인가 코드를 받아 처리하고, JWT 토큰을 쿠키에 저장한 후 프론트엔드로 리다이렉트합니다.

**요청:**
- 쿼리 파라미터: `code` (카카오 인가 코드)
- 헤더 (선택): `X-Frontend-Callback-Url` - 프론트엔드 콜백 URL

**응답:**
- 리다이렉트: `{frontendCallbackUrl}/login/kakao/callback`
- 쿠키 설정:
  - `Authorization`: JWT Access Token
  - `RefreshToken`: JWT Refresh Token

**상태 코드:**
- `302 Found`: 리다이렉트
- `500 Internal Server Error`: 서버 오류

---

### 5. 네이버 로그인

#### 5.1 로그인 URL 가져오기

**엔드포인트:** `GET /api/auth/naver/login`

**설명:** 네이버 인가 URL을 생성하여 반환합니다.

**요청:**
- 헤더 (선택): `X-Frontend-Callback-Url` - 프론트엔드 콜백 URL

**응답:**
```json
{
  "authUrl": "https://nid.naver.com/oauth2.0/authorize?..."
}
```

**상태 코드:**
- `200 OK`: 성공

#### 5.2 네이버 콜백 처리

**엔드포인트:** `GET /api/auth/naver/callback`

**설명:** 네이버 인가 코드를 받아 처리하고, JWT 토큰을 쿠키에 저장한 후 프론트엔드로 리다이렉트합니다.

**요청:**
- 쿼리 파라미터:
  - `code` (네이버 인가 코드)
  - `state` (상태 값)
- 헤더 (선택): `X-Frontend-Callback-Url` - 프론트엔드 콜백 URL
- 쿠키 (선택): `FrontendCallbackUrl` - 프론트엔드 콜백 URL (우선순위: 헤더 > 쿠키 > 환경 변수)

**응답:**
- 리다이렉트: `{frontendCallbackUrl}/login/naver/callback`
- 쿠키 설정:
  - `Authorization`: JWT Access Token
  - `RefreshToken`: JWT Refresh Token

**상태 코드:**
- `302 Found`: 리다이렉트
- `500 Internal Server Error`: 서버 오류

---

### 6. 구글 로그인

#### 6.1 로그인 URL 가져오기

**엔드포인트:** `GET /api/auth/google/login`

**설명:** 구글 인가 URL을 생성하여 반환합니다.

**요청:**
- 헤더 (선택): `X-Frontend-Callback-Url` - 프론트엔드 콜백 URL

**응답:**
```json
{
  "authUrl": "https://accounts.google.com/o/oauth2/v2/auth?..."
}
```

**상태 코드:**
- `200 OK`: 성공

#### 6.2 구글 콜백 처리

**엔드포인트:** `GET /api/auth/google/callback`

**설명:** 구글 인가 코드를 받아 처리하고, JWT 토큰을 쿠키에 저장한 후 프론트엔드로 리다이렉트합니다.

**요청:**
- 쿼리 파라미터: `code` (구글 인가 코드)
- 헤더 (선택): `X-Frontend-Callback-Url` - 프론트엔드 콜백 URL
- 쿠키 (선택): `FrontendCallbackUrl` - 프론트엔드 콜백 URL (우선순위: 헤더 > 쿠키 > 환경 변수)

**응답:**
- 리다이렉트: `{frontendCallbackUrl}/login/google/callback`
- 쿠키 설정:
  - `Authorization`: JWT Access Token
  - `RefreshToken`: JWT Refresh Token

**상태 코드:**
- `302 Found`: 리다이렉트
- `500 Internal Server Error`: 서버 오류

---

### 7. 로그인 로그 기록

**엔드포인트:** `POST /api/log/login`

**설명:** 로그인 액션을 로그로 기록합니다.

**요청:**
```json
{
  "action": "Gateway 카카오 연결 시작"
}
```

**응답:**
```json
{
  "success": true,
  "message": "로그가 기록되었습니다."
}
```

**상태 코드:**
- `200 OK`: 성공
- `500 Internal Server Error`: 서버 오류

---

## 쿠키 설정

### 쿠키 이름
- `Authorization`: JWT Access Token
- `RefreshToken`: JWT Refresh Token
- `FrontendCallbackUrl`: 프론트엔드 콜백 URL (임시 저장용)

### 쿠키 속성
- `httpOnly: true` - JavaScript 접근 차단 (XSS 방지)
- `secure: false` (개발) / `true` (프로덕션) - HTTPS에서만 전송
- `path: /` - 모든 경로에서 사용 가능
- `sameSite: Lax` (기본값) - CSRF 방지

---

## 프론트엔드 콜백 URL 우선순위

소셜 로그인 콜백 처리 시 프론트엔드 콜백 URL은 다음 우선순위로 결정됩니다:

1. **헤더** (`X-Frontend-Callback-Url`)
2. **쿠키** (`FrontendCallbackUrl`)
3. **환경 변수** (`FRONT_LOGIN_CALLBACK_URL`)

---

## 프론트엔드 연동 가이드

### 1. API Base URL 설정

```typescript
// lib/api.ts
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
```

### 2. 소셜 로그인 시작

```typescript
import { startSocialLogin } from '@/lib/api';

// 카카오 로그인
await startSocialLogin('kakao');

// 네이버 로그인
await startSocialLogin('naver');

// 구글 로그인
await startSocialLogin('google');
```

### 3. 인증 상태 확인

```typescript
const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
  credentials: 'include', // 쿠키 포함
});

if (response.ok) {
  const userData = await response.json();
  // userData.id 사용
}
```

### 4. 로그아웃

```typescript
await fetch(`${API_BASE_URL}/api/auth/logout`, {
  method: 'POST',
  credentials: 'include', // 쿠키 포함
});
```

### 5. 콜백 페이지 처리

콜백 페이지는 백엔드에서 자동으로 리다이렉트되므로, 단순히 인증 상태를 확인하면 됩니다:

```typescript
// app/login/kakao/callback/page.tsx
useEffect(() => {
  fetch(`${API_BASE_URL}/api/auth/me`, {
    credentials: 'include',
  })
    .then(res => {
      if (res.ok) {
        router.push('/login/dashboard');
      } else {
        router.push('/');
      }
    });
}, []);
```

---

## 현재 프론트엔드 경로 확인 사항

### ✅ 정상 동작하는 경로

1. **소셜 로그인 URL 요청**
   - 프론트엔드: `GET /api/auth/{provider}/login`
   - 백엔드: `GET /api/auth/{provider}/login`
   - ✅ 일치

2. **인증 상태 확인**
   - 프론트엔드: `GET /api/auth/me`
   - 백엔드: `GET /api/auth/me`
   - ✅ 일치

3. **로그아웃**
   - 프론트엔드: `POST /api/auth/logout`
   - 백엔드: `POST /api/auth/logout`
   - ✅ 일치

4. **로그인 로그 기록**
   - 프론트엔드: `POST /api/log/login`
   - 백엔드: `POST /api/log/login`
   - ✅ 일치

5. **콜백 경로**
   - 백엔드 리다이렉트: `/login/{provider}/callback`
   - 프론트엔드 페이지: `/app/login/{provider}/callback/page.tsx`
   - ✅ 일치

### ⚠️ 확인 필요 사항

1. **토큰 갱신 API 사용 여부**
   - 백엔드: `POST /api/auth/refresh`
   - 프론트엔드에서 사용하는지 확인 필요
   - 현재 프론트엔드 코드에서 확인되지 않음

2. **CORS 설정**
   - 프론트엔드 Origin이 백엔드 CORS 설정에 포함되어 있는지 확인
   - `credentials: 'include'` 사용 시 CORS 설정 필수

3. **환경 변수**
   - `NEXT_PUBLIC_API_BASE_URL` 설정 확인
   - 기본값: `http://localhost:8080`

---

## 변경 사항 없음

현재 프론트엔드(`www.kroaddy.site`)의 경로는 백엔드(`api.kroaddy.site`)의 API 엔드포인트와 정확히 일치하므로 **변경할 사항이 없습니다**.

### 현재 구조

```
프론트엔드                    백엔드
─────────────────────────────────────────────
GET /api/auth/me      →   GET /api/auth/me
POST /api/auth/logout →   POST /api/auth/logout
GET /api/auth/kakao/login → GET /api/auth/kakao/login
GET /api/auth/naver/login → GET /api/auth/naver/login
GET /api/auth/google/login → GET /api/auth/google/login
POST /api/log/login   →   POST /api/log/login

콜백 경로:
/login/kakao/callback → /login/kakao/callback
/login/naver/callback → /login/naver/callback
/login/google/callback → /login/google/callback
```

---

## 추가 권장 사항

1. **토큰 갱신 로직 추가**
   - Access Token 만료 시 자동으로 Refresh Token으로 갱신하는 로직 추가 권장
   - `POST /api/auth/refresh` 엔드포인트 활용

2. **에러 처리 강화**
   - 네트워크 오류, 인증 실패 등에 대한 사용자 친화적 에러 메시지 표시

3. **로딩 상태 관리**
   - 로그인 진행 중 로딩 상태 표시

4. **보안 강화**
   - 프로덕션 환경에서 `cookie.secure: true` 설정
   - HTTPS 사용 필수

