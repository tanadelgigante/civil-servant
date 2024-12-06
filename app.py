from flask import Flask, jsonify
import importlib
import json
import subprocess
import os

app = Flask(__name__)

# Application information
APP_NAME = "Civil Servant"
APP_VERSION = "1.0.0"
APP_AUTHOR = "@ilgigante77"
APP_WEBSITE = "http://example.com"

def load_modules():
    """
    Load and initialize modules dynamically based on configuration.
    """
    config_path = os.getenv('CONFIG_PATH', 'config/module_config.json')
    modules_path = os.getenv('MODULES_PATH', 'modules')

    print(f"[INFO] Loading modules from configuration file: {config_path}")
    print(f"[INFO] Modules path: {modules_path}")

    with open(config_path) as f:
        modules = json.load(f)['modules']
        for module in modules:
            # Set environment variables for the module
            print(f"[DEBUG] Setting environment variables for module: {module['name']}")
            for key, value in module.get('env', {}).items():
                os.environ[key] = value

            # Install module-specific requirements
            module_path = f"{modules_path}/{module['name']}"
            requirements_path = f"{module_path}/requirements.txt"
            if os.path.exists(requirements_path):
                print(f"[INFO] Installing requirements for module: {module['name']}")
                subprocess.run(['pip', 'install', '-r', requirements_path])

            # Dynamically import and register the module
            print(f"[INFO] Importing and registering module: {module['path']}")
            mod = importlib.import_module(module['path'])
            if hasattr(mod, 'register'):
                mod.register(app)
            else:
                print(f"[WARNING] Module {module['path']} does not have a register method")

@app.route('/')
def index():
    """
    Default route for the application.
    """
    return {"message": "Welcome to the generic API server!"}

if __name__ == '__main__':
    print(f"[INFO] Starting {APP_NAME} application")
    print(f"[INFO] Version: {APP_VERSION}")
    print(f"[INFO] Author: {APP_AUTHOR}")
    print(f"[INFO] Website: {APP_WEBSITE}")

    # Load and initialize modules
    load_modules()

    # Start the Flask application
    app.run(host='0.0.0.0', port=5000)
