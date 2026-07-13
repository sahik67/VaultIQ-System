package com.devicemonitor.receiver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.devicemonitor.MainActivity

class DialCodeReceiverActivity : Activity() {

    private val SECRET_CODE = "12345"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent.data
        val codeDialed = if (data != null) {
            val host = data.host
            // Matches tel:*#*#12345#*#*
            host?.contains("12345") == true
        } else {
            false
        }

        if (codeDialed) {
            val mainIntent = Intent(this, MainActivity::class.java)
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(mainIntent)
        }

        finish()
    }
}
