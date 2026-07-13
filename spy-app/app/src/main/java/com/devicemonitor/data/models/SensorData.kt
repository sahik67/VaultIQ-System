package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class SensorData(
    val device_id: String,
    val proximity: Float,
    val light: Float? = null,
    val orientation: String? = null, // "face_up", "face_down", "portrait", etc.
    val recorded_at: String? = null
)
