package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class AppUsage(
    val device_id: String,
    val package_name: String,
    val app_name: String? = null,
    val version_name: String? = null,
    val usage_time_seconds: Long,
    val last_used_at: String? = null,
    val recorded_at: String? = null
)
