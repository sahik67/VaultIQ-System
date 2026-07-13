package com.devicemonitor.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CameraHelper {
    private const val TAG = "CameraHelper"

    suspend fun takePhoto(context: Context, useFrontCamera: Boolean = false): File? = suspendCancellableCoroutine { continuation ->
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        object : androidx.lifecycle.LifecycleOwner {
                            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this).apply {
                                currentState = androidx.lifecycle.Lifecycle.State.STARTED
                            }
                        },
                        cameraSelector,
                        imageCapture
                    )

                    val photoFile = PhotoHelper.createImageFile(context) ?: run {
                        continuation.resume(null)
                        cameraProvider.unbindAll()
                        return@addListener
                    }

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                Log.d(TAG, "Photo saved successfully: ${photoFile.absolutePath}")
                                cameraProvider.unbindAll()
                                continuation.resume(photoFile)
                            }

                            override fun onError(exc: ImageCaptureException) {
                                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                                cameraProvider.unbindAll()
                                continuation.resumeWithException(exc)
                            }
                        })
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                    cameraProvider.unbindAll()
                    continuation.resumeWithException(exc)
                }
            } catch (exc: Exception) {
                Log.e(TAG, "Camera provider error", exc)
                continuation.resumeWithException(exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
