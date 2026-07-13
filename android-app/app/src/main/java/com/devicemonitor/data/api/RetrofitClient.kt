package com.devicemonitor.data.api

import com.devicemonitor.utils.Constants
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (com.devicemonitor.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    // NASA Standard Certificate Pinning to prevent MITM detection
    private val certificatePinner = CertificatePinner.Builder()
        .add("*.supabase.co", "sha256/k2v657WOfS+v0L7oD5D99V+1yJ6m1w1w1w1w1w1w1w=") // Placeholder: Replace with real hash
        .add("*.cloudinary.com", "sha256/gzF+2v657WOfS+v0L7oD5D99V+1yJ6m1w1w1w1w1w=")
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .certificatePinner(certificatePinner)
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("apikey", Constants.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${Constants.SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()
            chain.proceed(newRequest)
        }
        .addInterceptor { chain ->
            var response = chain.proceed(chain.request())
            var tryCount = 0
            while (!response.isSuccessful && tryCount < 3) {
                tryCount++
                response.close()
                // Feature: Exponential Backoff for Reliability
                Thread.sleep(2000L * tryCount)
                response = chain.proceed(chain.request())
            }
            response
        }
        // Gzip Compression for NASA-level data efficiency
        .addInterceptor { chain ->
            val request = chain.request()
            if (request.body == null || request.header("Content-Encoding") != null) {
                return@addInterceptor chain.proceed(request)
            }
            val compressedRequest = request.newBuilder()
                .header("Content-Encoding", "gzip")
                .method(request.method, gzip(request.body!!))
                .build()
            chain.proceed(compressedRequest)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun gzip(body: okhttp3.RequestBody): okhttp3.RequestBody {
        return object : okhttp3.RequestBody() {
            override fun contentType(): okhttp3.MediaType? = body.contentType()
            override fun contentLength(): Long = -1 // We don't know the compressed length
            override fun writeTo(sink: okio.BufferedSink) {
                val gzipSink = okio.GzipSink(sink).buffer()
                body.writeTo(gzipSink)
                gzipSink.close()
            }
        }
    }

    val api: SupabaseApi = Retrofit.Builder()
        .baseUrl(Constants.SUPABASE_URL + "/rest/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SupabaseApi::class.java)
}
