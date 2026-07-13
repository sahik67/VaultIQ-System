package com.devicemonitor.data.api

import com.devicemonitor.utils.Constants
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Hardened Retrofit Client
 * - Removed Certificate Pinning (to be configured with real certificates in production)
 * - Automatic Exponential Backoff
 * - Secure Header Management
 * 
 * NOTE: For production, add proper certificate pinning with real certificate hashes.
 * Example:
 * private val certificatePinner = CertificatePinner.Builder()
 *     .add("*.supabase.co", "sha256/REAL_CERTIFICATE_HASH_HERE")
 *     .build()
 */
object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("apikey", Constants.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${Constants.SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .build()
            
            var response = chain.proceed(newRequest)
            var tryCount = 0
            
            // Automatic Retry with Exponential Backoff for 5xx and 429 errors
            while (!response.isSuccessful && tryCount < 3 && (response.code == 429 || response.code >= 500)) {
                tryCount++
                response.close()
                val backoffDelay = (Math.pow(2.0, tryCount.toDouble()) * 1000).toLong()
                Thread.sleep(backoffDelay)
                response = chain.proceed(newRequest)
            }
            response
        }
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val api: SupabaseApi = Retrofit.Builder()
        .baseUrl(Constants.SUPABASE_URL + "/rest/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SupabaseApi::class.java)
}
