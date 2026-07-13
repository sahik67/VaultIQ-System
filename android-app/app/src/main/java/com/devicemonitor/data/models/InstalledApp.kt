package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class InstalledApp(
    val device_id: String,
    val package_name: String,
    val app_name: String,
    val version_name: String?,
    val is_system_app: Boolean,
    val recorded_at: String? = null
)
