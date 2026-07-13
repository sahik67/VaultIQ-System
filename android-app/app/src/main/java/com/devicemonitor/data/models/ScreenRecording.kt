package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ScreenRecording(
    val id: String? = null,
    val device_id: String,
    val cloudinary_public_id: String,
    val file_url: String? = null,
    val duration_seconds: Long? = null,
    val recorded_at: String? = null
)
