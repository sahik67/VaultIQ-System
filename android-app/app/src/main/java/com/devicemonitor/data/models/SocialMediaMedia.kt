package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class SocialMediaMedia(
    val device_id: String,
    val messenger_type: String,
    val media_type: String, // "image", "audio", "video"
    val file_url: String,
    val cloudinary_public_id: String,
    val contact_name: String? = null,
    val recorded_at: String? = null
)
