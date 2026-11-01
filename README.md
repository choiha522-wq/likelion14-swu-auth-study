# LIKELION 14기 2SWU 인증 스터디

주차별 브랜치는 각 주차의 실습 결과를 누적합니다. 실행에는 JDK 21과 Maven이 필요합니다.

## 1주차: 세션 기반 로그인 기초

`POST /session/login`에 `{"username":"study","password":"password"}`를 보내 로그인합니다. 브라우저/HTTP 클라이언트가 응답의 `JSESSIONID` 쿠키를 보관하면 다음 요청에서 같은 세션을 사용할 수 있습니다.

- `GET /session/me`: 세션 로그인 사용자 확인
- `POST /session/logout`: 서버 세션 무효화

고정 계정은 동작 원리 학습 전용이며 실제 서비스에 사용하면 안 됩니다.

```bash
mvn spring-boot:run
```
