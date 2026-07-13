package com.devicemonitor.data.models

import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName

@Serializable
data class Photo(
    @SerializedName("device_id") val device_id: String,
    @SerializedName("cloudinary_public_id") val cloudinary_public_id: String,
    @SerializedName("photo_url") val photo_url: String,
    @SerializedName("thumbnail_url") val thumbnail_url: String? = null,
    @SerializedName("file_size_bytes") val file_size_bytes: Long? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("mime_type") val mime_type: String? = null,
    @SerializedName("taken_at") val taken_at: String? = null,
    @SerializedName("photo_type") val photo_type: String? = null,
    @SerializedName("recorded_at") val recorded_at: String? = null
)
