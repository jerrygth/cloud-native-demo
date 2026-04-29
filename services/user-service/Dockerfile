FROM gradle:8.12-jdk21 AS builder

WORKDIR /build

COPY gradlew .
COPY gradle/wrapper/ gradle/wrapper/

COPY build.gradle settings.gradle ./
COPY gradle/libs.versions.toml gradle/libs.versions.toml

COPY buildSrc/ buildSrc/

COPY services/user-service/ services/user-service/

# Download dependencies first (excellent caching)
RUN ./gradlew dependencies --no-daemon

RUN ./gradlew :services:user-service:clean services:user-service:bootJar -x test --no-daemon


FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=builder /build/services/user-service/build/libs/*.jar app.jar

USER spring

EXPOSE 8081

ENV JAVA_OPTS="\
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]