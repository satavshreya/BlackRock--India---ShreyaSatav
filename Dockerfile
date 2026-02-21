# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# App must run on 5477 inside container
EXPOSE 5477

# Force server port to 5477
ENTRYPOINT ["java","-jar","app.jar","--server.port=5477"]