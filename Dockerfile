# Dockerfile
FROM eclipse-temurin:21-jdk as build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw .
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests
RUN ls -l /app/target  # Debug: verifica se il file JAR è presente nella directory target

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/civilservant-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
