package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class WebHistory(
    val device_id: String,
    val url: String,
    val title: String? = null,
    val entry_time: String? = null,
    val exit_time: String? = null,
    val duration_seconds: Long? = null,
    val browsing_mode: String = "standard", // "standard" or "incognito"
    val recorded_at: String? = null
)
