package com.devicemonitor.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ContactEntry(
    val device_id: String,
    val contact_id: String, // Matches schema's contact_id
    val display_name: String,
    val phone_numbers: List<String>? = null,
    val emails: List<String>? = null,
    val photo_uri: String? = null,
    val starred: Boolean = false,
    val organization: String? = null,
    val job_title: String? = null,
    val recorded_at: String? = null
)
