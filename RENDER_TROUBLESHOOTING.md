# 렌더 배포 문제 해결 가이드

## 🔍 일반적인 문제 및 해결 방법

### 1. 빌드 실패

#### 문제: "Java version not found" 또는 "Unsupported Java version"
**원인:** Java 버전이 명시되지 않음
**해결:**
- `render.yaml`에서 `runtime: java-21` 확인
- 또는 Render 대시보드에서 Runtime을 `Java 21`로 설정

#### 문제: "Permission denied" (Gradle 실행 오류)
**원인:** gradlew 실행 권한 없음
**해결:**
- `buildCommand`에 `chmod +x ./gradlew` 포함 (이미 포함됨)
- 또는 GitHub에 푸시하기 전 로컬에서 `chmod +x gradlew` 실행

#### 문제: "Dependency resolution failed"
**원인:** 의존성 다운로드 실패
**해결:**
- 빌드 로그 확인
- 네트워크 문제일 수 있으므로 재시도
- `build.gradle`의 repository 설정 확인

### 2. 실행 실패

#### 문제: "Port already in use" 또는 "Address already in use"
**원인:** 포트 설정 문제
**해결:**
- `application-prod.properties`에서 `server.port=${PORT}` 확인
- Start Command에 `-Dserver.port=$PORT` 포함 확인 (이미 포함됨)
- 렌더는 PORT 환경 변수를 자동 제공하므로 별도 설정 불필요

#### 문제: "Failed to bind to address" 또는 "Cannot assign requested address"
**원인:** 애플리케이션이 PORT 환경 변수를 읽지 못함
**해결:**
1. `application-prod.properties` 확인:
   ```properties
   server.port=${PORT}
   ```
2. Start Command 확인:
   ```bash
   java -Dserver.port=$PORT -jar ...
   ```

#### 문제: "Application failed to start" (데이터베이스 연결 오류)
**원인:** 데이터베이스 연결 정보 오류
**해결:**
1. 환경 변수 확인:
   ```
   SPRING_DATASOURCE_URL
   SPRING_DATASOURCE_USERNAME
   SPRING_DATASOURCE_PASSWORD
   ```
2. 데이터베이스 서비스가 실행 중인지 확인
3. Internal Database URL 사용 시 네트워크 문제 없음
4. External Database URL 사용 시:
   - 방화벽 설정 확인
   - SSL 연결 필요할 수 있음 (`?useSSL=true`)
   - 타임존 설정 추가 (`&serverTimezone=Asia/Seoul`)

#### 문제: "OutOfMemoryError"
**원인:** 메모리 부족
**해결:**
- Start Command에 JVM 옵션 추가:
  ```bash
  java -Xmx512m -Xms256m -Dserver.port=$PORT -jar ...
  ```
- 또는 Starter 플랜으로 업그레이드

### 3. Health Check 실패

#### 문제: Health Check가 계속 실패
**원인:** 애플리케이션이 루트 경로에서 응답하지 않음
**해결:**
1. `healthCheckPath: /` 설정 확인
2. 루트 경로(`/`)가 응답하는지 확인
3. Spring Security 설정에서 `/` 경로가 허용되어 있는지 확인
4. Health Check Path를 `/actuator/health`로 변경할 수 있지만, Actuator 의존성 필요

### 4. 데이터베이스 관련 문제

#### 문제: PostgreSQL 연결 실패 (MariaDB 사용 시)
**원인:** 드라이버 불일치
**해결:**
1. `build.gradle`에 PostgreSQL 의존성 추가:
   ```gradle
   runtimeOnly 'org.postgresql:postgresql'
   ```
2. `application-prod.properties`에서 드라이버 변경:
   ```properties
   spring.datasource.driver-class-name=org.postgresql.Driver
   ```

#### 문제: 스키마 초기화 실패
**원인:** SQL 스크립트 오류 또는 권한 문제
**해결:**
1. `spring.sql.init.continue-on-error=true` 설정 확인 (이미 포함됨)
2. SQL 스크립트 문법 확인
3. 데이터베이스 사용자 권한 확인

### 5. 환경 변수 관련 문제

#### 문제: 환경 변수가 적용되지 않음
**원인:** 변수명 오류 또는 설정 누락
**해결:**
1. 환경 변수 이름 확인:
   - Spring Boot는 `SPRING_DATASOURCE_URL` 형식 지원
   - 또는 `SPRING.DATASOURCE.URL` 형식도 가능
2. 렌더 대시보드에서 환경 변수 값 확인
3. 대소문자 구분 확인
4. 재배포 필요할 수 있음

### 6. Swagger 접근 불가

#### 문제: `/swagger-ui.html` 접근 불가
**원인:** 프로덕션 환경에서 Swagger 비활성화 또는 보안 설정
**해결:**
1. `application-prod.properties`에서 Swagger 활성화 확인:
   ```properties
   springdoc.swagger-ui.enabled=true
   springdoc.api-docs.enabled=true
   ```
2. `SecurityConfig.java`에서 경로 허용 확인:
   - `/swagger-ui/**`
   - `/api-docs/**`
3. 프로덕션 환경에서는 Swagger 비활성화 권장 (보안상)

## 📋 배포 체크리스트

배포 전 반드시 확인:

- [ ] `render.yaml` 파일이 루트 디렉토리에 있음
- [ ] `application-prod.properties` 파일 존재
- [ ] 로컬에서 빌드 테스트: `./gradlew clean bootJar`
- [ ] JAR 파일 생성 확인: `build/libs/PublicApiProj-0.0.1-SNAPSHOT.jar`
- [ ] 환경 변수 설정:
  - [ ] `SPRING_PROFILES_ACTIVE=prod`
  - [ ] `SPRING_DATASOURCE_URL` (데이터베이스 연결 정보)
  - [ ] `SPRING_DATASOURCE_USERNAME`
  - [ ] `SPRING_DATASOURCE_PASSWORD`
- [ ] 데이터베이스 서비스 생성 및 실행 확인
- [ ] GitHub에 최신 코드 푸시 완료

## 🔍 로그 확인 방법

1. **Render 대시보드 접속**
2. **"Logs" 탭 클릭**
3. **빌드 로그 확인:**
   - 빌드 과정 확인
   - 의존성 다운로드 상태 확인
   - 컴파일 에러 확인
4. **런타임 로그 확인:**
   - 애플리케이션 시작 로그
   - 데이터베이스 연결 로그
   - 에러 메시지 확인

## 💡 일반적인 에러 메시지 및 해결

### "Failed to start application"
→ 로그에서 구체적인 에러 메시지 확인

### "Connection refused"
→ 데이터베이스 연결 정보 확인

### "Table doesn't exist"
→ 스키마 초기화 확인

### "Access denied"
→ 데이터베이스 사용자 권한 확인

### "Port 8000 already in use"
→ `server.port=${PORT}` 설정 확인 (렌더는 자동으로 포트 할당)

## 📞 추가 도움말

- Render 공식 문서: https://render.com/docs
- Spring Boot 배포 가이드: https://spring.io/guides
- 문제가 계속되면 Render 지원팀에 문의

