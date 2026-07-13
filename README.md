# 🛡️ VaultIQ - Professional Stealth Monitoring & Control System

VaultIQ is an advanced, military-grade Android monitoring solution designed for high-performance and stealth. It consists of a target-side "Spy App", an administrator "Control App", a web-based "Dashboard", and a powerful "Telegram Bot".

---

## 🏗️ Project Architecture
The system follows a Centralized Command & Control (C2) architecture using **Supabase** as the backbone.

### 📁 Modules:
*   **[spy-app/](./spy-app)**: The core Android application for the target device. Features parallel sync, adaptive battery management, and 1.1M+ domain blocking.
*   **[control-app/](./control-app)**: A dedicated Android application for administrators to manage devices and send commands on the go.
*   **[tele-bot/](./tele-bot)**: A Python-based Telegram Master Controller for remote management via Telegram.
*   **[dashboard/](./dashboard)**: A high-performance web dashboard with real-time event engine and data analysis.
*   **[backend/](./backend)**: Database schemas, Row Level Security (RLS) policies, and automated pruning scripts.
*   **[porn-sites/](./porn-sites)**: A comprehensive database of 1.1M+ restricted domains used for the web filtering feature.

---

## 📚 Documentation
Here is a list of all technical documentation and guides available in this repository:

*   **[Feature Comparison Matrix](./ClevGuard_Feature_Comparison.md)**: A detailed comparison between VaultIQ and industry leaders like ClevGuard.
*   **[VPS Deployment Guide (Telegram Bot)](./tele-bot/setup_vps.md)**: Step-by-step instructions for hosting the Master Bot on a Hetzner VPS.
*   **[Executive Management Dashboard Design](./stitch-design-system/executive_management_dashboard/DESIGN.md)**: Design documentation and UI/UX prompts for the management interface.

---

## 📱 Core Features
*   **Ghost Mode Stealth**: App morphing (Calculator/Calendar shells) and exclusion from Recents.
*   **1.1M Domain Filter**: Automatic blocking of adult and restricted sites in real-time.
*   **Anti-Uninstall Guard**: Prevents users from deleting the app via Settings.
*   **Deep Message Extraction**: Scans screen text to capture encrypted chats from Telegram, WhatsApp, and Signal.
*   **Real-time Heartbeat**: 1-second precision online/offline status monitoring.
*   **Remote Power Control**: Fake Shutdown and Fake Reboot overlays to maintain persistence.

---

## 📱 Command Flow
`Control Interface (Bot/App/Web)` ➔ `Supabase Command Queue` ➔ `Spy App (Real-time Execution)` ➔ `Result Upload` ➔ `Live UI Update`

---

## ⚖️ Legal Disclaimer
This software is intended for legitimate parental monitoring or authorized enterprise use only. Unauthorized use of this tool for spying or data theft is strictly prohibited and may result in legal action.
