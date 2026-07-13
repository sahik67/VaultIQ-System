# VaultIQ - Ultimate Monitoring & Control System

## 🏗️ Architecture
The system follows a Centralized Command & Control (C2) architecture using Supabase as the backbone.

### 1. [spy-app](./spy-app)
The Android application installed on the target device. It collects data (Location, Calls, Messages, etc.) and executes remote commands.

### 2. [control-app](./control-app)
A dedicated mobile application for the administrator to monitor and send commands to devices. Mobile-optimized alternative to the dashboard.

### 3. [tele-bot](./tele-bot)
A Telegram Bot interface for quick control and real-time alerts. Commands like `/take_photo` can be sent directly from Telegram.

### 4. [dashboard](./dashboard)
The web-based administration panel for deep data analysis and full system management.

### 5. [backend](./backend)
Contains SQL schemas, RLS policies, and database configurations.

---

## 📱 Command Flow
`Control Interface (App/Bot/Web)` -> `Supabase DB` -> `Spy App (Realtime)` -> `Execution` -> `Result to DB` -> `Display`
