package com.devicemonitor.data.models

data class ClipboardEntry(
    val device_id: String,
    val content: String,
    val recorded_at: String? = null
)
