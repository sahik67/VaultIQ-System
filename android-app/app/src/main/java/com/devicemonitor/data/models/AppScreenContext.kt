package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class AppScreenContext(
    val device_id: String,
    val app_package: String,
    val app_name: String,
    val screen_text: String,
    val recorded_at: String? = null
)
