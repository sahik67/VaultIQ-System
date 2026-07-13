# Hetzner VPS Setup Guide for VaultIQ Bot

## 1. Install Dependencies
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install python3-pip python3-venv -y
```

## 2. Clone and Setup Environment
```bash
cd /opt
# Clone your private repo here
git clone https://github.com/sahik67/VaultIQ-System.git
cd VaultIQ-System/tele-bot

# Create virtual environment
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## 3. Create Systemd Service
Create a service file to keep the bot running 24/7:
```bash
sudo nano /etc/systemd/system/vaultiq-bot.service
```

Paste the following (Update paths and Token):
```ini
[Unit]
Description=VaultIQ Telegram Control Bot
After=network.target

[Service]
User=root
WorkingDirectory=/opt/VaultIQ-System/tele-bot
ExecStart=/opt/VaultIQ-System/tele-bot/venv/bin/python bot.py
Restart=always

[Install]
WantedBy=multi-user.target
```

## 4. Start the Bot
```bash
sudo systemctl daemon-reload
sudo systemctl enable vaultiq-bot
sudo systemctl start vaultiq-bot
sudo systemctl status vaultiq-bot
```
