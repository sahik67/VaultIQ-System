package com.devicemonitor

import android.app.Application
import com.devicemonitor.data.models.RiskAlert
import com.devicemonitor.data.repository.DeviceRepository
import com.devicemonitor.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonitorApplication : Application() {

    private val repository = DeviceRepository()
    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        
        // Global Crash Handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleGlobalException(thread, throwable)
        }
    }

    private fun handleGlobalException(thread: Thread, throwable: Throwable) {
        val deviceId = Constants.getDeviceId(this)
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        
        val alert = RiskAlert(
            device_id = deviceId,
            alert_type = "system_crash",
            severity = "critical",
            description = "App crashed on thread ${thread.name}: ${throwable.message}",
            content = throwable.stackTraceToString(),
            source = "MonitorApplication",
            recorded_at = timestamp
        )

        // Run in a blocking manner or a very fast coroutine since the app is dying
        applicationScope.launch {
            try {
                repository.insertRiskAlert(alert)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.invokeOnCompletion {
            // Let the system handle the crash normally after logging
            android.os.Process.killProcess(android.os.Process.myPid())
            java.lang.System.exit(10)
        }
    }
}
