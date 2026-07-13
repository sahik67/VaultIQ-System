package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class EmailEntry(
    val id: String? = null,
    val device_id: String,
    val from_address: String, // DB: NOT NULL
    val to_addresses: List<String>? = null,
    val cc_addresses: List<String>? = null,
    val bcc_addresses: List<String>? = null,
    val subject: String? = null,
    val body: String? = null,
    val is_read: Boolean = false,
    val email_timestamp: String, // DB: NOT NULL
    val recorded_at: String? = null
)
