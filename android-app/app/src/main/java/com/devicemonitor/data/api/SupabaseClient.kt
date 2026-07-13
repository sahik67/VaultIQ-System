package com.devicemonitor.data.api

import com.devicemonitor.utils.Constants
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = Constants.SUPABASE_URL,
        supabaseKey = Constants.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        defaultSerializer = KotlinXSerializer()
    }
}
