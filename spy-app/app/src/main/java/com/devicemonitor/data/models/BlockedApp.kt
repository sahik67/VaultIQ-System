package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class BlockedApp(
    val device_id: String,
    val package_name: String,
    val app_name: String? = null,
    val created_at: String? = null
)
