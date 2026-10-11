# Fase di build
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Fase di runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/civilservant*.jar app.jar
VOLUME /config
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=/config/application.yml"]
