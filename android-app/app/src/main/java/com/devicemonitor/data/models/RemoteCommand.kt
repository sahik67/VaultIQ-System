package com.devicemonitor.data.models

import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName

@Serializable
data class RemoteCommand(
    @SerializedName("id") val id: String? = null,
    @SerializedName("device_id") val device_id: String,
    @SerializedName("command") val command: String,
    @SerializedName("payload") val payload: String? = null,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("result") val result: String? = null,
    @SerializedName("executed_at") val executed_at: String? = null,
    @SerializedName("recorded_at") val recorded_at: String? = null,
    @SerializedName("created_at") val created_at: String? = null,
    @SerializedName("updated_at") val updated_at: String? = null
)
