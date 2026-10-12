package com.sipsmile.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.sipsmile.app.data.PrefsRepository
import com.sipsmile.app.widget.SipSmileWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime

private const val TAG = "SipSmile"

/** Runs [block] off the main thread while keeping the receiver alive (goAsync). */
private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try {
            block()
        } catch (t: Throwable) {
            Log.e(TAG, "Receiver work failed", t)
        } finally {
            pending.finish()
        }
    }
}

/** Fires at every reminder slot: deliver the reminder (if inside the window), then chain the next alarm. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        runAsync {
            val repo = PrefsRepository(app)
            val prefs = repo.read()
            if (prefs.onboardingDone && !prefs.paused) {
                val now = LocalTime.now()
                val nowMin = now.hour * 60 + now.minute
                // small grace so a slightly late alarm still counts, but never reminds outside the window
                if (nowMin >= prefs.startMinutes - 1 && nowMin <= prefs.endMinutes + 5) {
                    ReminderEngine.deliver(app)
                } else {
                    Log.d(TAG, "Alarm outside the active window, skipped")
                }
            }
            Scheduler.scheduleNext(app, repo.read())
        }
    }
}

/** Restores the alarm chain after reboot, app update, clock change or time-zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        runAsync {
            val repo = PrefsRepository(app)
            Notifier.createChannels(app)
            Scheduler.scheduleNext(app, repo.read())
            SipSmileWidget().updateAll(app)
        }
    }
}

/** "I drank" action button on the notification. */
class DrinkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        runAsync {
            PrefsRepository(app).logGlass()
            Notifier.cancel(app)
            SipSmileWidget().updateAll(app)
        }
    }
}

/** Midnight: refresh the widget (daily counter resets) and set up tomorrow's alarms. */
class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        runAsync {
            val repo = PrefsRepository(app)
            SipSmileWidget().updateAll(app)
            Scheduler.scheduleNext(app, repo.read())
        }
    }
}
