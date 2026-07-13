package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Keystroke(
    val device_id: String,
    val text_content: String,
    val app_name: String? = null,
    val package_name: String? = null,
    val keystroke_timestamp: String, // DB: NOT NULL
    val recorded_at: String? = null
)
