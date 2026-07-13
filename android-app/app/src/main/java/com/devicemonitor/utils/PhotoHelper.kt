package com.devicemonitor.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils.asMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoHelper {

    private const val TAG = "PhotoHelper"

    fun createImageFile(context: Context): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.getExternalFilesDir(null)
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image file", e)
            null
        }
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun uploadToCloudinary(file: File): Pair<String?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val cloudinary = Cloudinary(asMap(
                    "cloud_name", Constants.CLOUDINARY_CLOUD_NAME,
                    "api_key", Constants.CLOUDINARY_API_KEY,
                    "api_secret", Constants.CLOUDINARY_API_SECRET
                ))

                val uploadResult = cloudinary.uploader().upload(file, asMap("resource_type", "image"))
                Pair(
                    uploadResult["secure_url"] as? String,
                    uploadResult["public_id"] as? String
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading to Cloudinary", e)
                Pair(null, null)
            }
        }
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
