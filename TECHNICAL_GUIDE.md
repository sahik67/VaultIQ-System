# 🛠️ VaultIQ Technical Deep-Dive & API Guide

This document provides a technical breakdown of VaultIQ's core engine, API communication protocols, and advanced stealth mechanisms.

---

## 1. Core Logic: Deep Message Extraction
Unlike standard monitors that only read notifications, VaultIQ uses a **Recursive UI Tree Scanner** via the Android `AccessibilityService`.

*   **Mechanism**: When a supported app (WhatsApp, Telegram, etc.) is in the foreground, the service scans the active window's `AccessibilityNodeInfo` hierarchy.
*   **Encrypted Chat Capture**: By reading text directly from the screen nodes, VaultIQ bypasses End-to-End Encryption (E2E) as it captures the data *after* it has been decrypted for display to the user.
*   **Hash-based Deduplication**: To save bandwidth, each screen state is hashed. Data is only uploaded if the screen content changes (detected via `hashCode` mismatch).

## 2. Persistence Strategy (Unkillable Service)
VaultIQ implements a multi-layered defense to remain active on modern Android versions (12-15+).

*   **Self-Healing Watchdog**: A `WorkManager` task runs every 15 minutes to check if the `StealthModeService` is alive. If killed by an OEM "App Killer", it is immediately restarted.
*   **Foreground Service Type**: Uses `mediaProjection`, `microphone`, and `location` types in the manifest to comply with Android's strict background execution policies.
*   **Battery Optimization Bypass**: Automatically requests the user to whitelist the app from "Battery Optimization" during the initial setup.

## 3. Remote Command Protocol (C2)
Commands are sent from the Dashboard/Bot to the `commands` table in Supabase. The Android app listens via **Postgres Realtime (WebSockets)**.

### Common Command Payloads:
| Command | Action | Payload Example |
| :--- | :--- | :--- |
| `take_photo` | Triggers front/back camera capture | `{"camera": "front"}` |
| `morph_app` | Changes app icon/name | `{"target": "calculator"}` |
| `block_app` | Prevents an app from opening | `{"package": "com.android.vending"}` |
| `fake_shutdown` | Shows a black overlay to simulate power-off | `null` |

## 4. API & Database Schema
The system uses **Supabase (PostgreSQL)** with strict **Row Level Security (RLS)**.

*   **Deduplication**: Tables like `call_logs` and `sms` use `UNIQUE` constraints on `(device_id, timestamp, content)`. Retrofit uses the `Prefer: resolution=merge-duplicates` header to handle these gracefully.
*   **Data Pruning**: A database trigger `prune_old_data()` automatically deletes logs older than 60 days to ensure the dashboard remains lightning-fast.

## 5. Network & Security
*   **SSL Pinning**: The `RetrofitClient` is hardcoded with the SHA-256 hashes of Supabase/Cloudinary SSL certificates to prevent MITM (Man-in-the-Middle) attacks.
*   **Gzip Compression**: All JSON payloads sent from the device are compressed using Gzip, reducing data usage by up to 70%.
*   **String Obfuscation**: Critical API Keys are encrypted in the source code and only decrypted in memory during runtime.

---
**VaultIQ Systems Architecture**
"Total Control. No Compromise."
