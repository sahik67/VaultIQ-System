package com.devicemonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MediaWorkerService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(20002, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_MIRROR" -> startMirroringLoop()
            "STOP_MIRROR" -> stopMirroringLoop()
        }
        return START_STICKY
    }

    private var isMirroring = false

    private fun startMirroringLoop() {
        if (isMirroring) return
        isMirroring = true
        // MediaProjection logic would be here in a real separate process
        // For now, it stays for stability reference
    }

    private fun stopMirroringLoop() {
        isMirroring = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("media_worker_channel", "Media Sync", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "media_worker_channel")
            .setContentTitle("Media Optimization")
            .setContentText("Syncing media data...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
