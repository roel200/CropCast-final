package com.cropcast.app.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cropcast.app.MainActivity
import com.cropcast.app.R
import com.cropcast.app.data.LocalPreferences
import com.cropcast.app.ui.localization.localize

object Notifier {
    private const val CHANNEL_ID = "farm_alerts"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val language = LocalPreferences(context).language
        val channel = NotificationChannel(CHANNEL_ID, localize("Farm alerts", language), NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** [message] is the English alert text the function sends; it is translated to the phone's chosen language here. */
    fun show(context: Context, type: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val language = LocalPreferences(context).language
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localize("CropCast farm alert", language))
            .setContentText(localize(message, language))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // One notification per alert type, so a repeat replaces the old one instead of stacking.
        NotificationManagerCompat.from(context).notify(type.hashCode(), notification)
    }
}
