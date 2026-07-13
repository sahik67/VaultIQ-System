package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val id: String? = null,
    val device_name: String,
    val device_token: String,
    val device_model: String? = null,
    val os_version: String? = null,
    val battery_level: Int? = null,
    val is_charging: Boolean? = null,
    val last_seen: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)
