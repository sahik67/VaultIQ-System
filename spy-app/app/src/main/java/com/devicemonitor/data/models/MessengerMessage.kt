package com.devicemonitor.data.models

import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName

@Serializable
data class MessengerMessage(
    @SerializedName("device_id") val device_id: String,
    @SerializedName("messenger_type") val messenger_type: String, 
    @SerializedName("conversation_id") val conversation_id: String? = null,
    @SerializedName("contact_name") val contact_name: String? = null,
    @SerializedName("contact_username") val contact_username: String? = null,
    @SerializedName("content") val content: String,
    @SerializedName("media_url") val media_url: String? = null,
    @SerializedName("media_type") val media_type: String? = null, 
    @SerializedName("message_type") val message_type: String, 
    @SerializedName("is_read") val is_read: Boolean? = null,
    @SerializedName("is_deleted") val is_deleted: Boolean? = null,
    @SerializedName("message_timestamp") val message_timestamp: String,
    @SerializedName("recorded_at") val recorded_at: String? = null
)
