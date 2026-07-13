package com.devicemonitor.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.devicemonitor.data.models.SimChange
import com.devicemonitor.data.repository.DeviceRepository
import com.devicemonitor.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimStateReceiver : BroadcastReceiver() {

    private val TAG = "SimStateReceiver"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = DeviceRepository()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.SIM_STATE_CHANGED") {
            Log.d(TAG, "SIM state changed")

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "READ_PHONE_STATE permission not granted")
                return
            }

            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            try {
                @Suppress("DEPRECATION")
                val currentImsi = telephonyManager.subscriberId

                if (currentImsi != null) {
                    val lastImsi = Constants.getLastImsi(context)

                    if (lastImsi != null && lastImsi != currentImsi) {
                        // SIM changed!
                        Log.d(TAG, "SIM card changed from $lastImsi to $currentImsi")

                        val simChange = SimChange(
                            device_id = Constants.getDeviceId(context),
                            old_imsi = lastImsi,
                            new_imsi = currentImsi,
                            recorded_at = SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                Locale.getDefault()
                            ).format(Date())
                        )

                        serviceScope.launch {
                            try {
                                repository.insertSimChange(simChange)
                                Log.d(TAG, "SIM change recorded successfully")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error inserting SIM change", e)
                            }
                        }

                        // Update last IMSI
                        Constants.setLastImsi(context, currentImsi)
                    } else if (lastImsi == null) {
                        // First time, just store the IMSI
                        Constants.setLastImsi(context, currentImsi)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting IMSI", e)
            }
        }
    }
}
