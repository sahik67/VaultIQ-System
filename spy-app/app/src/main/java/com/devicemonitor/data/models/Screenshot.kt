package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Screenshot(
    val device_id: String,
    val cloudinary_public_id: String,
    val screenshot_url: String,
    val thumbnail_url: String? = null,
    val file_size_bytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val taken_at: String? = null,
    val recorded_at: String? = null
)
