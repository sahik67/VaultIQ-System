package com.devicemonitor.data.models

import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName

@Serializable
data class SmsEntry(
    @SerializedName("device_id") val device_id: String,
    @SerializedName("contact_name") val contact_name: String? = null,
    @SerializedName("phone_number") val phone_number: String,
    @SerializedName("message_type") val message_type: String, 
    @SerializedName("content") val content: String,
    @SerializedName("is_read") val is_read: Boolean? = null,
    @SerializedName("is_deleted") val is_deleted: Boolean? = null,
    @SerializedName("sms_timestamp") val sms_timestamp: String,
    @SerializedName("recorded_at") val recorded_at: String? = null
)
