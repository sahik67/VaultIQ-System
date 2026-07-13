package com.devicemonitor.data.models

import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName

@Serializable
data class CallLogEntry(
    @SerializedName("device_id") val device_id: String,
    @SerializedName("contact_name") val contact_name: String? = null,
    @SerializedName("phone_number") val phone_number: String,
    @SerializedName("call_type") val call_type: String, 
    @SerializedName("duration_seconds") val duration_seconds: Int? = null,
    @SerializedName("call_timestamp") val call_timestamp: String,
    @SerializedName("is_read") val is_read: Boolean? = null,
    @SerializedName("recorded_at") val recorded_at: String? = null
)
