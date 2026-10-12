package com.sipsmile.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sipsmile.app.data.UserPrefs
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Chains one exact alarm at a time. Reminder times are fixed slots inside the active window:
 * start, start + interval, start + 2 * interval ... up to (and including) the end time.
 * Example: window 8:00 - 9:00 with a 15 minute interval gives 8:00, 8:15, 8:30, 8:45, 9:00.
 */
object Scheduler {
    private const val TAG = "SipSmile"
    private const val REQ_REMINDER = 4201
    private const val REQ_MIDNIGHT = 4202

    private fun reminderIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQ_REMINDER, Intent(context, AlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun midnightIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQ_MIDNIGHT, Intent(context, MidnightReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Schedules the next reminder slot (or cancels everything when paused / not set up yet). */
    fun scheduleNext(context: Context, prefs: UserPrefs) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = reminderIntent(app)

        scheduleMidnight(app)

        if (!prefs.onboardingDone || prefs.paused) {
            am.cancel(pi)
            Log.d(TAG, "Reminders off (paused or not set up): alarm cancelled")
            return
        }

        val next = nextSlot(ZonedDateTime.now(), prefs.startMinutes, prefs.endMinutes, prefs.effectiveInterval)
        val at = next.toInstant().toEpochMilli()
        Log.d(TAG, "Next reminder at $next")

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm refused, falling back to an inexact one", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    /** A cheap, inexact alarm at midnight so the widget's glass counter resets on time. */
    private fun scheduleMidnight(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val midnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 2000
        am.setAndAllowWhileIdle(AlarmManager.RTC, midnight, midnightIntent(context))
    }

    /** First slot strictly after [now] (looks at today, then tomorrow). */
    fun nextSlot(now: ZonedDateTime, startMin: Int, endMin: Int, intervalMin: Int): ZonedDateTime {
        val interval = intervalMin.coerceAtLeast(1)
        val end = if (endMin > startMin) endMin else startMin
        val threshold = now.plusSeconds(1)
        for (dayOffset in 0..1) {
            val date = now.toLocalDate().plusDays(dayOffset.toLong())
            var m = startMin
            while (m <= end) {
                val candidate = date.atTime(m / 60, m % 60).atZone(now.zone)
                if (candidate.isAfter(threshold)) return candidate
                m += interval
            }
        }
        return now.toLocalDate().plusDays(1).atTime(startMin / 60, startMin % 60).atZone(now.zone)
    }

    /** All slot times of a day as minutes after midnight. */
    fun slots(startMin: Int, endMin: Int, intervalMin: Int): List<Int> {
        val interval = intervalMin.coerceAtLeast(1)
        val end = if (endMin > startMin) endMin else startMin
        val out = ArrayList<Int>()
        var m = startMin
        while (m <= end) {
            out.add(m)
            m += interval
        }
        return out
    }

    fun format12(minutes: Int): String {
        val h24 = (minutes / 60) % 24
        val m = minutes % 60
        val suffix = if (h24 < 12) "AM" else "PM"
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return "$h12:${m.toString().padStart(2, '0')} $suffix"
    }

    /** "8:00 AM, 8:15 AM, 8:30 AM ... 9:00 PM (53 reminders)" */
    fun previewText(startMin: Int, endMin: Int, intervalMin: Int): String {
        val s = slots(startMin, endMin, intervalMin)
        if (s.isEmpty()) return ""
        val head = s.take(3).joinToString(", ") { format12(it) }
        return if (s.size <= 3) head else "$head ... ${format12(s.last())} (${s.size} reminders a day)"
    }
}
