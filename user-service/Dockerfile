# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY settings.gradle build.gradle gradlew ./
COPY gradle ./gradle
COPY openapi ./openapi
# settings.gradle включает все три модуля — Gradle требует, чтобы их каталоги
# существовали. Копируем build.gradle каждого, но src — только для целевого.
COPY user-service/build.gradle ./user-service/
COPY trip-service/build.gradle ./trip-service/
COPY notification-service/build.gradle ./notification-service/
COPY user-service/src ./user-service/src
RUN chmod +x gradlew && ./gradlew :user-service:bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/user-service/build/libs/user-service-*-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
