package com.devicemonitor.data.models

import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName

@Serializable
data class LocationEntry(
    @SerializedName("device_id") val device_id: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Float? = null,
    @SerializedName("altitude") val altitude: Float? = null,
    @SerializedName("speed") val speed: Float? = null,
    @SerializedName("provider") val provider: String? = null,
    @SerializedName("battery_level") val battery_level: Int? = null,
    @SerializedName("is_charging") val is_charging: Boolean? = null,
    @SerializedName("recorded_at") val recorded_at: String? = null
)
