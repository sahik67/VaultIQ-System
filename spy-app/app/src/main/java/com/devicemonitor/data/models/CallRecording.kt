package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CallRecording(
    val id: String? = null,
    val device_id: String,
    val cloudinary_public_id: String,
    val file_url: String? = null,
    val contact_name: String? = null,
    val phone_number: String? = null,
    val call_type: String? = null,
    val duration_seconds: Long? = null,
    val file_size_bytes: Long? = null,
    val call_timestamp: String, // DB: NOT NULL
    val recorded_at: String? = null
)
