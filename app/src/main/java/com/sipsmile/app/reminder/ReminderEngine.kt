package com.sipsmile.app.reminder

import android.appwidget.AppWidgetManager
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.sipsmile.app.AppState
import com.sipsmile.app.data.Messages
import com.sipsmile.app.data.PrefsRepository
import com.sipsmile.app.data.ReminderType
import com.sipsmile.app.data.Roster
import com.sipsmile.app.widget.SipSmileWidget
import com.sipsmile.app.widget.SipSmileWidgetReceiver
import java.time.LocalTime

/** Everything that happens when a reminder is due. */
object ReminderEngine {
    private const val TAG = "SipSmile"

    /**
     * @param forced   send exactly this type (used by the test buttons)
     * @param isTest   test reminders always show a notification so you can see how it looks and sounds
     */
    suspend fun deliver(context: Context, forced: ReminderType? = null, isTest: Boolean = false) {
        val app = context.applicationContext
        val repo = PrefsRepository(app)
        val prefs = repo.read()

        val type = forced ?: if (prefs.lastType == ReminderType.WATER.name) ReminderType.SMILE else ReminderType.WATER
        val hour = LocalTime.now().hour
        val characterId = Roster.pick(type, hour, prefs.lastCharacter)
        val lastIdx = if (type == ReminderType.WATER) prefs.lastWaterIdx else prefs.lastSmileIdx
        val (idx, message) = Messages.pick(type, prefs.name, lastIdx)

        repo.recordReminder(type, characterId, message, idx)

        // the widget always gets the new character + message
        try {
            SipSmileWidget().updateAll(app)
        } catch (e: Exception) {
            Log.w(TAG, "Widget update failed", e)
        }

        val screenOffOrLocked = isScreenOffOrLocked(app)
        val shouldNotify = isTest ||
            screenOffOrLocked ||
            !hasWidget(app) ||
            (prefs.notifyWhenScreenOn && !AppState.foreground)

        Log.d(TAG, "Reminder $type via $characterId | screenOff=$screenOffOrLocked notify=$shouldNotify")
        if (shouldNotify) {
            Notifier.show(app, repo.read(), type, characterId, message)
        }
    }

    private fun isScreenOffOrLocked(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return !pm.isInteractive || km.isKeyguardLocked
    }

    fun hasWidget(context: Context): Boolean {
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, SipSmileWidgetReceiver::class.java))
        return ids.isNotEmpty()
    }
}
