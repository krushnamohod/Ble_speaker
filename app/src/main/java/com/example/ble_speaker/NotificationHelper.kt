package com.example.ble_speaker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val NOTIFICATION_ID = 1001
    private const val CHANNEL_ID = "AudioStreamChannel"

    fun createNotification(context: Context, isMuted: Boolean = false): Notification {
        createNotificationChannel(context)

        val muteIntent = Intent(context, AudioStreamService::class.java).apply {
            action = AudioStreamService.ACTION_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            context,
            0,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteActionTitle = if (isMuted) "Unmute" else "Mute"

        val stopIntent = Intent(context, AudioStreamService::class.java).apply {
            action = AudioStreamService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // The intent to open the app when the notification is clicked
        // Note: Assumes MainActivity will be at this package location
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            2,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("BLE Speaker Streaming")
            .setContentText(if (isMuted) "Microphone is muted" else "Microphone is live and streaming")
            .setSmallIcon(android.R.drawable.stat_sys_headset) 
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_media_play, muteActionTitle, mutePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Changed to LOW for Foreground service standard
            .build()
    }

    fun updateNotification(context: Context, isMuted: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(context, isMuted))
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bluetooth Audio Stream",
                NotificationManager.IMPORTANCE_LOW 
            ).apply {
                description = "Shows the active audio streaming status"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
