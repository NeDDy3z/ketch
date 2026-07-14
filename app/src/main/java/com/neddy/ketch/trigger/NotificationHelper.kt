package com.neddy.ketch.trigger

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.neddy.ketch.MainActivity
import com.neddy.ketch.R
import com.neddy.ketch.domain.model.Watcher

class NotificationHelper(private val context: Context) {

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_CONNECTIONS,
            "Connection alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Fastest public transport connection when a watcher fires"
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Posts the connection notification for [watcher]. The [title] carries
     * the first boarding with its departure time, [text] the rest of the
     * route collapsed to one line, and [expandedText] the same stops on
     * separate lines.
     */
    fun notifyConnection(watcher: Watcher, title: String, text: String, expandedText: String) {
        if (!hasPermission()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            watcher.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CONNECTIONS)
            .setSmallIcon(R.drawable.ic_train)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_TAG, watcher.id.toInt(), notification)
    }

    companion object {
        const val CHANNEL_CONNECTIONS = "connections"
        private const val NOTIFICATION_TAG = "watcher_connection"
    }
}
