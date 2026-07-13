package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class AppTraffic(
    val device_id: String,
    val package_name: String,
    val app_name: String,
    val bytes_sent: Long,
    val bytes_received: Long,
    val recorded_at: String? = null
)
