FROM amazoncorretto:17-alpine AS builder
WORKDIR /app
COPY . .
# Gradle Wrapper가 없으므로 로컬 Gradle 빌드 대신 Docker 내에서 Gradle을 설치하거나 Wrapper를 다운로드해야 함.
# 하지만 여기서는 간편함을 위해 gradle을 직접 설치하여 빌드.
RUN apk add --no-cache gradle
RUN gradle build -x test --no-daemon

FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
