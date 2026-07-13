package com.devicemonitor.data.models

import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName

@Serializable
data class DeviceInfo(
    @SerializedName("device_id") val device_id: String,
    @SerializedName("model") val model: String,
    @SerializedName("android_version") val android_version: String,
    @SerializedName("ram_available") val ram_available: Long,
    @SerializedName("ram_total") val ram_total: Long,
    @SerializedName("storage_available") val storage_available: Long,
    @SerializedName("storage_total") val storage_total: Long,
    @SerializedName("recorded_at") val recorded_at: String? = null
)
