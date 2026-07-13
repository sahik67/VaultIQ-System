package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class SimChange(
    val device_id: String,
    val old_imsi: String? = null,
    val new_imsi: String? = null,
    val recorded_at: String? = null
)
