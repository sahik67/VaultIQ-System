package com.devicemonitor.utils

import android.util.Base64
import java.nio.charset.Charset

/**
 * Advanced String Obfuscation to bypass Static Analysis scanners.
 * Uses XOR + Base64 to hide sensitive keys and API names.
 */
object StringObfuscator {
    private const val KEY = "NASA_ULTRA_SECRET_KEY"

    fun decrypt(input: String): String {
        val decoded = Base64.decode(input, Base64.DEFAULT)
        val result = ByteArray(decoded.size)
        for (i in decoded.indices) {
            result[i] = (decoded[i].toInt() xor KEY[i % KEY.length].toInt()).toByte()
        }
        return String(result, Charset.forName("UTF-8"))
    }

    // Use this to generate obfuscated strings for your code
    fun encrypt(input: String): String {
        val result = ByteArray(input.length)
        for (i in input.indices) {
            result[i] = (input[i].toInt() xor KEY[i % KEY.length].toInt()).toByte()
        }
        return Base64.encodeToString(result, Base64.DEFAULT)
    }
}
