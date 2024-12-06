# Civil Servant

## Overview
**Civil Servant** is a modular API server built with Flask, designed to dynamically load and initialize modules based on configuration. This project enables you to extend the server's functionality by adding custom modules with ease.

## Features
- **Dynamic Module Loading**: Load and initialize modules dynamically based on a JSON configuration file.
- **Modular Architecture**: Extend the server's capabilities by adding new modules without modifying the core server code.
- **Environment Variable Management**: Each module can have its own set of environment variables.
- **Automatic Dependency Installation**: Install module-specific dependencies automatically.

## Application Information
- **Name**: Civil Servant
- **Version**: 1.0.0
- **Author**: Your Name
- **Website**: [http://example.com](http://example.com)

## Getting Started

### Prerequisites
- Python 3.9+
- Flask 2.1.0
- Docker (optional)

### Installation

1. **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/civil-servant.git
    cd civil-servant
    ```

2. **Install the required Python packages**:
    ```bash
    pip install -r requirements.txt
    ```

### Configuration

1. **Create the Configuration File**:
   Create a `config/module_config.json` file with the following structure:
    ```json
    {
      "modules": [
        {
          "name": "module1",
          "path": "modules.module1",
          "env": {
            "MODULE1_VAR1": "value1",
            "MODULE1_VAR2": "value2"
          }
        },
        {
          "name": "module2",
          "path": "modules.module2",
          "env": {
            "MODULE2_VAR1": "value3",
            "MODULE2_VAR2": "value4"
          }
        }
      ]
    }
    ```

2. **Module Structure**:
   Each module should be structured as a Python package with an `__init__.py` and a `register` function. For example:
    ```
    modules/
    ├── module1/
    │   ├── __init__.py
    │   ├── module1.py
    │   ├── requirements.txt
    ├── module2/
    │   ├── __init__.py
    │   ├── module2.py
    │   ├── requirements.txt
    ```

### Running the Server

1. **Run Locally**:
    ```bash
    python app.py
    ```

2. **Using Docker**:
   Create a `Dockerfile`:
    ```dockerfile
    FROM python:3.9-slim

    WORKDIR /app

    COPY . .

    RUN pip install --no-cache-dir -r requirements.txt

    EXPOSE 5000

    CMD ["python", "app.py"]
    ```

   Build and run the Docker container:
    ```bash
    docker build -t civil-servant .
    docker run -p 5000:5000 civil-servant
    ```

### Usage

#### API Endpoints

- **GET /**:
    ```
    curl http://localhost:5000/
    ```

    Response:
    ```json
    {
      "message": "Welcome to the generic API server!"
    }
    ```

### Debugging

- The server provides debug information in the console output. Look for `[INFO]`, `[DEBUG]`, and `[WARNING]` messages to understand the server's behavior and troubleshoot issues.

### Contributing
Contributions are welcome! Please fork the repository and submit pull requests for any enhancements or bug fixes.

### License
This project is licensed under the GPL 3.0 License. See the [LICENSE](LICENSE) file for details.

## Contact
For more information, visit [http://example.com](http://example.com) or contact [Your Name](mailto:youremail@example.com).
