# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY settings.gradle build.gradle ./
COPY gradle ./gradle
COPY gradlew ./
COPY openapi ./openapi
COPY trip-service/build.gradle ./trip-service/
COPY trip-service/src ./trip-service/src
RUN chmod +x gradlew && ./gradlew :trip-service:bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/trip-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
