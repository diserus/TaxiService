# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY settings.gradle build.gradle gradlew ./
COPY gradle ./gradle
COPY openapi ./openapi
COPY user-service/build.gradle ./user-service/
COPY trip-service/build.gradle ./trip-service/
COPY notification-service/build.gradle ./notification-service/
COPY trip-service/src ./trip-service/src
RUN chmod +x gradlew && ./gradlew :trip-service:bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/trip-service/build/libs/trip-service-*-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
