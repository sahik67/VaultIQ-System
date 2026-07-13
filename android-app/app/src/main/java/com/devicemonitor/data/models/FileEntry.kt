package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class FileEntry(
    val device_id: String,
    val file_path: String,
    val file_name: String,
    val is_directory: Boolean,
    val size_bytes: Long = 0,
    val last_modified: String? = null,
    val recorded_at: String? = null
)
