FROM gradle:8.12-jdk21 AS build
WORKDIR /src
COPY . .
RUN gradle :app-api:bootJar --no-daemon

FROM eclipse-temurin:21-jre
COPY --from=build /src/app-api/build/libs/*.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx768m", "-jar", "/app.jar"]
