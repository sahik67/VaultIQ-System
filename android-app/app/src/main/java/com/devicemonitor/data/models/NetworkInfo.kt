package com.devicemonitor.data.models

import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName

@Serializable
data class NetworkInfo(
    @SerializedName("device_id") val device_id: String,
    @SerializedName("wifi_ssid") val wifi_ssid: String? = null,
    @SerializedName("network_type") val network_type: String? = null,
    @SerializedName("signal_strength") val signal_strength: Int? = null,
    @SerializedName("is_wifi_connected") val is_wifi_connected: Boolean = false,
    @SerializedName("cell_id") val cell_id: String? = null,
    @SerializedName("location_area_code") val location_area_code: String? = null,
    @SerializedName("mobile_country_code") val mobile_country_code: String? = null,
    @SerializedName("mobile_network_code") val mobile_network_code: String? = null,
    @SerializedName("recorded_at") val recorded_at: String? = null
)
