FROM gradle:8.12-jdk21 AS build
WORKDIR /src
COPY . .
RUN gradle :app-collector:bootJar --no-daemon

FROM eclipse-temurin:21-jre
COPY --from=build /src/app-collector/build/libs/*.jar /app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx768m", "-jar", "/app.jar"]
