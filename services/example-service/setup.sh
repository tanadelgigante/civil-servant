###ESEMPIO PYTHON

#!/bin/bash

# Update and install dependencies
apt-get update && apt-get install -y python3 python3-pip build-essential && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
if [ -f "requirements.txt" ]; then
  pip3 install -r requirements.txt
fi

# Run the application
python3 app.py

###ESEMPIO NODE.JS

#!/bin/bash

# Update and install dependencies
apt-get update && apt-get install -y curl build-essential && rm -rf /var/lib/apt/lists/*
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt-get install -y nodejs

# Install Node.js dependencies
if [ -f "package.json" ]; then
  npm install
fi

# Run the application
node main.js
