package com.sipsmile.app.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.sipsmile.app.MainActivity
import com.sipsmile.app.R
import com.sipsmile.app.data.ReminderType
import com.sipsmile.app.data.Roster
import com.sipsmile.app.data.UserPrefs

object Notifier {
    const val NOTIFICATION_ID = 1001
    const val SUMMARY_ID = 1002

    private val vibrationPattern = longArrayOf(0, 120, 90, 120)

    /** One channel per sound + vibration combination, because Android fixes a channel's sound once it exists. */
    fun channelId(sound: String, vibrate: Boolean): String = "ss_${sound}_${if (vibrate) "vib" else "quiet"}"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sounds = listOf(
            Triple("chime", "Soft chime", R.raw.chime_soft),
            Triple("tweet", "Bird tweet", R.raw.bird_tweet),
            Triple("none", "Silent", 0),
        )
        for ((key, label, res) in sounds) {
            for (vib in listOf(false, true)) {
                val channel = NotificationChannel(
                    channelId(key, vib),
                    if (vib) "$label + vibration" else label,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.description = "Gentle water and smile reminders"
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                channel.setShowBadge(false)
                if (res != 0) {
                    channel.setSound(Uri.parse("android.resource://${context.packageName}/$res"), attrs)
                } else {
                    channel.setSound(null, null)
                }
                channel.enableVibration(vib)
                if (vib) channel.vibrationPattern = vibrationPattern
                nm.createNotificationChannel(channel)
            }
        }
    }

    /** The intent that opens the full-screen animated scene (used by the notification and by the widget). */
    fun sceneIntent(context: Context, characterId: String, type: ReminderType, message: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_SCENE, true)
            putExtra(MainActivity.EXTRA_CHARACTER, characterId)
            putExtra(MainActivity.EXTRA_TYPE, type.name)
            putExtra(MainActivity.EXTRA_MESSAGE, message)
        }

    fun show(context: Context, prefs: UserPrefs, type: ReminderType, characterId: String, message: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundKey = if (prefs.soundOn) prefs.soundChoice else "none"
        val channel = channelId(soundKey, prefs.vibrationOn)

        val openPi = PendingIntent.getActivity(
            context, 77, sceneIntent(context, characterId, type, message),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val name = prefs.name.trim().ifBlank { "Friend" }
        val title = when (type) {
            ReminderType.WATER -> "$name, water time 💧"
            ReminderType.SMILE -> "$name, smile time 😊"
            ReminderType.COMBO -> "$name, sip & smile 💧😊"
        }

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF5BB5F0.toInt())
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPi)

        characterBitmap(context, characterId)?.let { builder.setLargeIcon(it) }

        val drinkPi = PendingIntent.getBroadcast(
            context, 78, Intent(context, DrinkReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val smilePi = PendingIntent.getBroadcast(
            context, 79, Intent(context, SmileReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePi = PendingIntent.getBroadcast(
            context, 80, Intent(context, SnoozeReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (type != ReminderType.SMILE) builder.addAction(R.drawable.ic_notification, "I drank 💧", drinkPi)
        if (type != ReminderType.WATER) builder.addAction(R.drawable.ic_notification, "😊 Smiled", smilePi)
        builder.addAction(R.drawable.ic_notification, "Snooze 10m", snoozePi)

        nm.notify(NOTIFICATION_ID, builder.build())
    }

    /** The quiet end-of-day summary. */
    fun showSummary(context: Context, prefs: UserPrefs) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val name = prefs.name.trim().ifBlank { "Friend" }
        val smiles = if (prefs.smilesToday == 1) "1 smile" else "${prefs.smilesToday} smiles"
        val text = buildString {
            append("You drank ${prefs.progressText} glasses (${com.sipsmile.app.data.formatMl(prefs.drankMlToday)}) and shared $smiles today. ")
            append(
                when {
                    prefs.goalReached -> "Goal reached, well done! 🎉"
                    prefs.glassesToday == 0 -> "Tomorrow is a fresh start 🌅"
                    else -> "So close! Tomorrow we go again 💪"
                }
            )
        }
        val openPi = PendingIntent.getActivity(
            context, 81, Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, channelId("none", false))
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF5BB5F0.toInt())
            .setContentTitle("$name, your day in sips & smiles 🌙")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .build()
        nm.notify(SUMMARY_ID, n)
    }

    private fun characterBitmap(context: Context, characterId: String): Bitmap? = try {
        ContextCompat.getDrawable(context, Roster.widgetImage(characterId, happy = false))?.toBitmap(160, 160)
    } catch (e: Exception) {
        null
    }

    fun cancel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }
}
