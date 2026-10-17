package com.eliteguardian.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.eliteguardian.app.MainActivity
import com.eliteguardian.app.R

class StreakReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Fully neutral, privacy-respecting notification (no explicit or sensitive words)
        val notification = NotificationCompat.Builder(context, "elite_guardian_streak")
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(context.getString(R.string.streak_reminder_title))
            .setContentText(context.getString(R.string.streak_reminder_desc))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(STREAK_NOTIFICATION_ID, notification)
    }

    companion object {
        const val STREAK_NOTIFICATION_ID = 2002
    }
}

