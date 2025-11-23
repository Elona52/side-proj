# Java 21 기반 Spring Boot 애플리케이션 Dockerfile
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Gradle wrapper 및 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Gradle 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드 및 빌드 (캐시 최적화를 위해 분리)
RUN ./gradlew clean bootJar -x test --no-daemon

# 실행 단계
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/PublicApiProj-0.0.1-SNAPSHOT.jar app.jar

# 포트 노출 (렌더가 자동으로 PORT 환경 변수 제공)
EXPOSE 8080

# 애플리케이션 실행 (쉘 형태로 변경하여 환경 변수 사용)
# SPRING_PROFILES_ACTIVE는 Render 환경 변수에서 설정됨
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar app.jar"]

