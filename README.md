# Civil Servant

## Overview
**Civil Servant** is a modular API gateway built with Spring Boot and Spring Cloud Gateway. It is designed to dynamically discover, register, and proxy services, enabling seamless integration of new services while providing a central access point.

## Features
- **Dynamic Service Discovery**: Automatically discovers services with `service-config.json` files in a designated directory.
- **Service Registration**: Registers services dynamically and proxies requests to them.
- **Route Management**: Automatically configures routes based on service descriptors.
- **Modular Architecture**: Easily extend functionality by adding new services or enhancing the gateway logic.
- **Environment Variable and Configuration Support**: Services can include their own configuration and setup scripts.

## Application Information
- **Name**: Civil Servant
- **Version**: 2.0.0 (vedi `version.ini`, unica sorgente di verita')
- **Author**: @ilgigante77
- **Website**: [https://github.com/tanadelgigante/civil-servant](https://github.com/tanadelgigante/civil-servant)

## Getting Started

### Prerequisites
- Java 25 (LTS)
- Maven 3.9+
- Docker (optional for containerized deployment)

### Installation

1. **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/civil-servant.git
    cd civil-servant
    ```

2. **Build the application**:
    ```bash
    mvn clean install
    ```

3. **Create the services directory**:
    Create a `services/` directory in the project root. Each service should have its own subdirectory with a `service-config.json` file and optional `setup.sh` script.

### Configuration

1. **Define Routes and Services**:
   Ensure each service directory contains a `service-config.json`. The gateway accetta
   sia il campo legacy `start_command` (snake_case) sia `startCommand` (camelCase),
   con precedenza `start.command` > `startCommand` > `start_command`. Esempio legacy:
    ```json
    {
      "name": "sample-api",
      "language": "python",
      "route": "/sample-api",
      "start_command": "your_start_command_here"
    }
    ```

2. **Service Setup**:
   Each service directory can include an optional `setup.sh` script to prepare its environment (e.g., install dependencies).

3. **Application Configuration**:
   Update `config/application.yml` as needed to customize gateway routes or application settings:
    ```yaml
    server:
      port: 8187

    logging:
      level:
        root: INFO
        com.tanadelgigante: DEBUG

    spring:
      cloud:
        gateway:
          server:
            webflux:
              routes:
                - id: sample-api
                  uri: http://127.0.0.1:8000
                  predicates:
                    - Path=/sample-api/**
                  filters:
                    - StripPrefix=1
    ```

### Running the Application

1. **Run Locally**:
    ```bash
    mvn spring-boot:run
    ```

2. **Using Docker**:
   The `Dockerfile` (multi-stage) builds the JAR with a Maven image pinned to Java 25 and runs it on `eclipse-temurin:25-jre`:
    ```dockerfile
    FROM maven:3.9.11-eclipse-temurin-25 AS build
    WORKDIR /app
    COPY pom.xml .
    COPY src ./src
    RUN mvn package -DskipTests

    FROM eclipse-temurin:25-jre
    WORKDIR /app
    COPY --from=build /app/target/civilservant*.jar app.jar
    VOLUME /config
    ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=/config/application.yml"]
    ```

   Build and run the Docker container:
    ```bash
    docker build -t civil-servant .
    docker run -p 8187:8187 civil-servant
    ```

   > ⚠️ `docker-compose.yaml` e `.env.example` sono **esempi generici**: mostrano
   > la forma, non i valori reali. I valori reali (path, credenziali, proprieta')
   > li definisce l'installatore nella propria installazione. Vedi
   > [`docs/architecture.md`](docs/architecture.md).

3. **Service Execution**:
   - Ensure services have valid configurations and scripts.
   - The gateway will automatically discover and start services using the start
     command (`start.command` > `startCommand` > `start_command`).

### Usage

#### API Endpoints
- **Health Check**:
    ```bash
    curl http://localhost:8187/test
    ```
    Response:
    ```json
    {
      "message": "Civil Servant Gateway is working!"
    }
    ```

- **Proxy Requests**:
    - Example: Access the Calibre API:
      ```bash
      curl -X GET http://localhost:8187/sample-api/endpoint?api_token=your_32_char_token
      ```

### Debugging

- Use the application logs to monitor service registration and route configuration. Look for `[INFO]` and `[DEBUG]` messages in the console output.
- To debug individual services, check their logs during startup.

### Contributing
Contributions are welcome! Fork the repository and submit pull requests for enhancements or bug fixes.

### License
This project is licensed under the GPL 3.0 License. See the [LICENSE](LICENSE) file for details.

### Disclaimer
This project is released "as-is" and the author is not responsible for damage, errors, or misuse.

## Contact
For more information, visit [https://github.com/tanadelgigante/civil-servant](https://github.com/tanadelgigante/civil-servant).
