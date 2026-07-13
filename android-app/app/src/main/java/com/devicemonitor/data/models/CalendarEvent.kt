package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CalendarEvent(
    val device_id: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val start_time: String,
    val end_time: String,
    val recorded_at: String? = null
)
