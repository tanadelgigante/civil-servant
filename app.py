from flask import Flask, jsonify
import importlib
import json
import subprocess
import os

app = Flask(__name__)

def load_modules():
    config_path = os.getenv('CONFIG_PATH', 'config/module_config.json')
    modules_path = os.getenv('MODULES_PATH', 'modules')

    with open(config_path) as f:
        modules = json.load(f)['modules']
        for module in modules:
            # Impostare le variabili d'ambiente
            for key, value in module.get('env', {}).items():
                os.environ[key] = value

            # Installare i requisiti specifici del modulo
            module_path = f"{modules_path}/{module['name']}"
            requirements_path = f"{module_path}/requirements.txt"
            if os.path.exists(requirements_path):
                subprocess.run(['pip', 'install', '-r', requirements_path])

            # Caricare dinamicamente il modulo come pacchetto
            mod = importlib.import_module(module['path'])
            if hasattr(mod, 'register'):
                mod.register(app)

@app.route('/')
def index():
    return {"message": "Welcome to the generic API server!"}

if __name__ == '__main__':
    load_modules()
    app.run(host='0.0.0.0', port=5000)
