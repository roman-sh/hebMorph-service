# --- Build Stage ---
FROM gradle:7.6-jdk11 AS build
WORKDIR /home/gradle/src
COPY . .
RUN gradle shadowJar --no-daemon

# --- Run Stage ---
FROM openjdk:11.0.15-jre-slim
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/hebmorph-service.jar .
EXPOSE 5001
CMD ["java", "-jar", "hebmorph-service.jar"] 