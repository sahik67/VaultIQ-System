package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class WifiHistory(
    val device_id: String,
    val ssid: String,
    val bssid: String,
    val first_seen: String? = null,
    val last_seen: String? = null,
    val recorded_at: String? = null
)
