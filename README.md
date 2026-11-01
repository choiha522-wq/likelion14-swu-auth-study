# LIKELION 14기 2SWU 인증 스터디

주차별 브랜치는 각 주차의 실습 결과를 누적합니다. 실행에는 JDK 21과 Maven이 필요합니다.

## 1주차: 세션 기반 로그인 기초

`POST /session/login`에 `{"username":"study","password":"password"}`를 보내 로그인합니다. 브라우저/HTTP 클라이언트가 응답의 `JSESSIONID` 쿠키를 보관하면 다음 요청에서 같은 세션을 사용할 수 있습니다.

- `GET /session/me`: 세션 로그인 사용자 확인
- `POST /session/logout`: 서버 세션 무효화

고정 계정은 1주차 동작 원리 학습 전용이며 실제 서비스에 사용하면 안 됩니다.

## 2주차: Spring Security 세션 로그인

`POST /signup`에 JSON `{"username":"tester","password":"password123"}`를 보내 가입한 다음, `/login`의 기본 로그인 폼에서 로그인합니다. 로그인 성공 후 세션 쿠키가 발급되고 `/account/me`에서 인증 사용자와 권한을 확인할 수 있습니다. 사용자 저장소는 프로세스 메모리이므로 서버를 재시작하면 가입 정보가 사라집니다.

CSRF 방어를 켜 둔 상태입니다. JSON POST 요청에는 `XSRF-TOKEN` 쿠키 값을 `X-XSRF-TOKEN` 헤더로 보내야 합니다. 브라우저 폼 로그인에는 Spring Security 기본 로그인 페이지를 사용할 수 있습니다.

## 3주차: JWT 구조와 발급

`POST /auth/login`에 가입한 계정 정보를 보내면 HS256 서명 Access Token을 받습니다. `GET /auth/verify` 요청의 `Authorization: Bearer <토큰>` 헤더로 서명과 만료를 확인합니다. 현재 단계에서는 검증 API로 토큰 자체를 학습하며, JWT를 보호 API의 인증 수단으로 연결하는 작업은 4주차에서 합니다.

JWT 서명 비밀 값은 `JWT_SECRET` 환경변수로 지정합니다. 로컬 기본값은 학습 편의를 위한 값이므로 실제 서비스에서는 사용하면 안 됩니다. 이 단계도 Spring Security의 CSRF 보호가 활성화되어 있어 `/auth/login` POST에는 CSRF 쿠키와 헤더가 필요합니다.

## 4주차: JWT 필터와 권한별 접근

JWT 전용 경로는 `/api/**`로 분리했습니다. `/api/auth/login`에서 발급한 토큰을 `Authorization: Bearer <토큰>`으로 보내면 필터가 검증하고 사용자·역할을 SecurityContext에 넣습니다. 세션 실습 경로의 설정을 바꾸지 않도록 JWT API에만 무상태 보안 체인을 적용했습니다.

- `GET /api/account/me`: 인증 필요, `USER` 이상
- `GET /api/admin/ping`: `ADMIN` 역할 필요
- 토큰 누락·만료·위조는 401, 권한 부족은 403 JSON 응답
- 가입한 계정은 `USER` 역할을 받습니다. 기본 메모리 계정 `admin / admin1234`는 `USER`, `ADMIN` 역할을 가집니다.

## 5주차: Refresh Token 재발급·로그아웃

로그인 응답은 Access Token을 JSON으로 반환하고, Refresh Token은 `HttpOnly`, `Secure`, `SameSite=Strict` 쿠키로 전달합니다. `/api/auth/reissue`는 서명·만료·토큰 종류를 확인하고 인메모리 저장소의 SHA-256 해시와 대조한 뒤 새 Access/Refresh Token을 발급합니다. 성공하면 저장된 Refresh Token 해시를 원자적으로 교체하므로 이전 토큰 재사용은 실패합니다. `/api/auth/logout`은 서버 저장값을 지우고 쿠키를 만료시킵니다.

재발급·로그아웃 요청에는 `X-Refresh-Request: true` 헤더를 함께 보내야 합니다. Refresh 쿠키는 브라우저가 자동 전송하므로 `SameSite=Strict`와 사용자 정의 헤더를 사용하며, 다른 출처를 허용하는 CORS 설정을 추가하지 않습니다. Refresh Token 저장소는 프로세스 메모리 기반이라 재시작 시 지워지며 다중 서버 운영용이 아닙니다.

로컬 HTTP 테스트에서 쿠키가 전송되지 않는 경우, `JWT_COOKIE_SECURE=false`를 로컬 전용으로 설정할 수 있습니다. HTTPS가 아닌 공개·배포 환경에서 Secure를 끄면 안 됩니다.

```bash
mvn spring-boot:run
```
