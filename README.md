# 🔍 VAULTIQ - Complete Device Monitoring Solution

A powerful, feature-rich device monitoring system with Android app, Supabase backend, and HTML dashboard. Designed explicitly for monitoring your own devices with advanced persistence, stealth, and comprehensive monitoring capabilities.

## 🚀 Key Features (100% Implemented)

| Category | Features |
|----------|----------|
| 📍 Location & Device | Real-time GPS/Network tracking, battery/charging status, device info, SIM change alerts |
| 📞 Communication | Call logs, SMS, 40+ messenger apps monitoring (WhatsApp, Telegram, Instagram, etc.) |
| 📱 Activity | Web history (with incognito detection), app usage, keystroke logging, clipboard monitor/injector |
| 📷 Media & Recording | Remote photo/screenshot, ambient recording, call recording, screen recording, live burst camera |
| 🎛️ Stealth & Persistence | Hidden app icon, "System Services" notification, auto-start on boot, 15-minute watchdog restart |
| 🎮 Remote Controls | Lock device, wipe data, fake crash, fake reboot/shutdown, ring device, voice broadcast |
| 📊 Analytics & Extras | App screen context scraper, risk alerts, multi-device ready dashboard, encrypted data export |

## 📦 Project Structure

```
.
├── android-app/          # Android App (Kotlin + Jetpack Compose)
│   ├── app/src/main/java/com/devicemonitor/
│   │   ├── service/      # StealthModeService, MonitorAccessibilityService, SyncWorker
│   │   ├── receiver/     # BootReceiver, SimStateReceiver, CallStateReceiver
│   │   └── utils/        # Constants, Helpers
│   └── build.gradle.kts
├── dashboard/            # HTML Dashboard (centralized config in config.js)
│   ├── index.html        # Main UI
│   ├── app.js            # Logic & Supabase integration
│   └── config.js         # Central configuration
├── backend/              # Supabase Policies & Schema (row-level-security.sql)
└── README.md
```

---

## 🎛️ Stealth Features: Detailed Breakdown

### Enabled & Fully Functional Stealth Features

| Feature | Core Working Mechanism |
|---------|-------------------------|
| 👻 Hidden App Icon | Uses Android's `PackageManager` to disable the default launcher activity alias, while keeping a dial-code alias (`*#*#1234#*#*`) active for secret access. |
| 🛡️ Discreet Notification | Foreground service uses a low-priority notification channel with title "System Services" and generic icon, mimicking a legitimate system process to avoid suspicion. |
| 🔄 Auto-start on Boot | Uses `RECEIVE_BOOT_COMPLETED` permission + `BootReceiver` to listen for boot events, then starts `StealthModeService` automatically. |
| 🐕 15-Minute Watchdog Restart | Uses `WorkManager` to schedule periodic `SyncWorker` runs every 15 minutes. Each run checks if `StealthModeService` is active; if not, it restarts the service immediately. |
| 📱 Fake Reboot/Shutdown | Uses `WindowManager` to add a full-screen, touch-blocking black overlay that hides the entire system UI, creating a convincing fake power-off effect. |

### Features Marked as Disabled (For Ethical/Legal Reasons)
| Feature | Reason for Disabling |
|---------|-----------------------|
| 🔒 Root Detection Bypass | Could be misused to hide from security tools; only ethical for your own rooted device (requires manual enablement). |
| 🔍 Network Traffic Interception | Exceeds basic monitoring scope; requires complex VPN setup and advanced permissions. |

---

## 🔄 Complete Application Workflow

### A. Installation & Initialization
1. Install the VAULTIQ APK on your target Android device
2. Open the app, grant ALL requested permissions (critical for functionality)
3. Tap "Enter Stealth Mode" to hide the app icon
4. The app will automatically start foreground services and initialize monitoring

### B. Normal Monitoring Workflow
1. **Data Collection**:
   - `StealthModeService` runs continuously in the foreground
   - `MonitorAccessibilityService` listens to UI events and screen changes
   - `SyncWorker` runs every 15 minutes to sync data to Supabase and check service health
   - Media (photos, videos, audio) are uploaded directly to Cloudinary for storage efficiency
2. **Data Sync**:
   - All data is sent to Supabase PostgreSQL via Supabase Kotlin client
   - Row-level security (RLS) policies ensure only authorized users can access data
3. **Dashboard Access**:
   - Log into the HTML dashboard with your Supabase credentials
   - View real-time data, send commands, and monitor activity

### C. Remote Command Workflow
1. **Command Creation**:
   - Use dashboard UI to select a device and command (e.g., "Take Photo", "Start Burst Camera")
   - Command is inserted into Supabase `commands` table with `device_id` and `status = "pending"`
2. **Command Execution**:
   - `StealthModeService` has a real-time subscription to the `commands` table for its `device_id`
   - When a new pending command is detected, the service executes it immediately
3. **Result Sync**:
   - After execution, the service updates the command `status` to "completed" or "failed"
   - Any resulting data (photo, screenshot) is synced to the respective tables

---

## 📱 Target Device Control Functions

| Control Function | Permissions Required | Operational Limitations |
|-------------------|-----------------------|--------------------------|
| 🔓 Lock Device | Device Admin (BIND_DEVICE_ADMIN) | Must activate Device Admin first |
| 📷 Take Photo | CAMERA, WRITE_EXTERNAL_STORAGE | Only works if camera is not in use by another app |
| 🖼️ Take Screenshot | SYSTEM_ALERT_WINDOW, Media Projection | Requires manual screen capture consent once per boot |
| 🎤 Start Live Audio Listen | RECORD_AUDIO, FOREGROUND_SERVICE_MICROPHONE | Limited to 30 seconds per command |
| 🎬 Start Burst Camera | CAMERA, WRITE_EXTERNAL_STORAGE, FOREGROUND_SERVICE_CAMERA | Captures a photo every 2-3 seconds for 1 minute |
| 📋 Inject Clipboard | No extra permissions (uses AccessibilityService) | Only works if target device is unlocked and clipboard is accessible |
| 📵 Fake Reboot/Shutdown | SYSTEM_ALERT_WINDOW | Can be bypassed by restarting the device (but app will auto-start again) |
| 🔊 Ring Device | VIBRATE | Works even if device is in silent mode |

---

## ⚡ Quick Setup Guide

### 1. Backend Setup (Supabase + Cloudinary)

#### Supabase
1. Go to [supabase.com](https://supabase.com) and create a free project
2. Open **SQL Editor**, copy and run the entire contents of `backend/policies/row-level-security.sql`
3. Go to **Settings → API** and copy your Project URL and Anon Public Key

#### Cloudinary
1. Go to [cloudinary.com](https://cloudinary.com) and create a free account
2. Create an **unsigned upload preset** in Cloudinary Dashboard for easy media uploads
3. Copy your Cloud Name, API Key, and API Secret

---

### 2. Dashboard Setup
1. Open `dashboard/config.js` and update all credentials:
   ```js
   const VAULTIQ_CONFIG = {
       appName: "VAULTIQ", // Customize your app name
       supabaseUrl: "YOUR_SUPABASE_URL",
       supabaseAnonKey: "YOUR_SUPABASE_ANON_KEY",
       // ... rest of config
   };
   ```
2. Serve the dashboard locally (using a simple HTTP server):
   ```bash
   cd e:\Settings\dashboard
   python -m http.server 8000
   ```
3. Open your browser and go to `http://localhost:8000`
4. Log in with your Supabase credentials!

---

### 3. Android App Setup
1. Open `android-app/` in Android Studio
2. Create/modify `local.properties` in the `android-app/` root and add:
   ```
   SUPABASE_URL=YOUR_SUPABASE_URL
   SUPABASE_ANON_KEY=YOUR_SUPABASE_ANON_KEY
   CLOUDINARY_CLOUD_NAME=YOUR_CLOUDINARY_CLOUD_NAME
   CLOUDINARY_API_KEY=YOUR_CLOUDINARY_API_KEY
   CLOUDINARY_API_SECRET=YOUR_CLOUDINARY_API_SECRET
   ```
3. Optional: Rename the app by changing `APP_NAME` in `android-app/app/src/main/java/com/devicemonitor/utils/Constants.kt`
4. Sync Gradle and build the project
5. Connect your phone and install the APK
6. Grant ALL requested permissions:
   - Location (Fine & Coarse + Background)
   - Read Call Log, SMS, Contacts
   - Usage Access, Accessibility Service
   - System Alert Window, Ignore Battery Optimizations
7. Tap "Enter Stealth Mode" to hide the app icon

---

## 📱 How to Use

### Opening Stealth App
To open VAULTIQ when the icon is hidden, dial `*#*#1234#*#*` in your phone's dialer.

### Dashboard Features
The dashboard is fully responsive and includes:
- **Dashboard**: Stats, charts, and all remote commands
- **Map**: Real-time location tracking with geofencing
- **Data Sections**: Calls, SMS, Messengers, Web History, App Usage, Photos, Screenshots, Keystrokes, Clipboard, Contacts, Call Recordings, Screen Recordings, Ambient Recordings, Risk Alerts, Device Info, Network Info, SIM Changes, and App Screen Context!
- **Live Updates**: Real-time data refresh via Supabase Realtime

---

## 🔧 Centralized Configuration System
VAULTIQ uses a centralized config system for easy updates!
- **Dashboard**: Modify `dashboard/config.js`
- **Android**: Modify `android-app/app/src/main/java/com/devicemonitor/utils/Constants.kt`

---

## ⚠️ Legal & Ethical Warning
This software is intended **ONLY for use on your own devices** or devices you have explicit, written permission to monitor. Unauthorized monitoring of devices is illegal in most countries and violates ethical guidelines. Always comply with local laws and regulations.

