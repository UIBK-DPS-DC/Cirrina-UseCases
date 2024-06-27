#!/bin/bash

# Check if the script is executed as root or with sudo
if [ "$EUID" -ne 0 ]; then
    echo "Please run this script as root or with sudo."
    exit 1
fi

# Check if requirements.txt exists
if [ ! -f "requirements.txt" ]; then
    echo "Error: requirements.txt not found. Make sure it exists in the current directory."
    exit 1
fi

# Install dependencies
pip install -r requirements.txt

# Create directory and copy files
mkdir /opt/csm-service-mockcamera
cp src/service.py /opt/csm-service-mockcamera/csm-service-mockcamera.py && cp -r resources/ /opt/csm-service-mockcamera/

# Copy systemd service file
cp csm-service-mockcamera.service /etc/systemd/system/

# Reload systemd and enable/start the service
systemctl daemon-reload
systemctl enable csm-service-mockcamera.service
systemctl start csm-service-mockcamera.service

# Check if the service is running
if systemctl is-active --quiet csm-service-mockcamera.service; then
    echo "Service is running."
else
    echo "Error: Failed to start the service. Please check the logs (journalctl -t csm-service-mockcamera) for more information."
    exit 1
fi

echo "Installation completed successfully."