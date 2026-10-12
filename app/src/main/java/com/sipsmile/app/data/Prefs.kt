package com.sipsmile.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.sipDataStore: DataStore<Preferences> by preferencesDataStore(name = "sip_smile_prefs")

/** Everything the app remembers. All values are already "resolved" (e.g. glasses reset on a new day). */
data class UserPrefs(
    val name: String = "",
    val onboardingDone: Boolean = false,
    val startMinutes: Int = 8 * 60,
    val endMinutes: Int = 21 * 60,
    val intervalMinutes: Int = 15,
    val debugFast: Boolean = false,
    val paused: Boolean = false,
    val soundOn: Boolean = true,
    val soundChoice: String = "chime",      // "chime" or "tweet"
    val vibrationOn: Boolean = false,
    val notifyWhenScreenOn: Boolean = false,
    val lastType: String = "SMILE",         // WATER / SMILE alternate
    val lastCharacter: String = "",
    val lastMessage: String = "",
    val lastWaterIdx: Int = -1,
    val lastSmileIdx: Int = -1,
    val lastReminderEpoch: Long = 0L,
    val lastDrinkEpoch: Long = 0L,
    val glassesToday: Int = 0,
    val streak: Int = 0,
    val goal: Int = 8,
) {
    /** The interval actually used for scheduling (1 minute when the testing switch is on). */
    val effectiveInterval: Int get() = if (debugFast) 1 else intervalMinutes
}

class PrefsRepository(context: Context) {
    private val store = context.applicationContext.sipDataStore

    private object K {
        val NAME = stringPreferencesKey("name")
        val ONBOARDED = booleanPreferencesKey("onboarding_done")
        val START = intPreferencesKey("window_start_min")
        val END = intPreferencesKey("window_end_min")
        val INTERVAL = intPreferencesKey("interval_min")
        val DEBUG_FAST = booleanPreferencesKey("debug_fast")
        val PAUSED = booleanPreferencesKey("paused")
        val SOUND_ON = booleanPreferencesKey("sound_on")
        val SOUND_CHOICE = stringPreferencesKey("sound_choice")
        val VIBRATION = booleanPreferencesKey("vibration_on")
        val NOTIFY_ON = booleanPreferencesKey("notify_when_screen_on")
        val LAST_TYPE = stringPreferencesKey("last_type")
        val LAST_CHAR = stringPreferencesKey("last_character")
        val LAST_MSG = stringPreferencesKey("last_message")
        val LAST_WATER_IDX = intPreferencesKey("last_water_idx")
        val LAST_SMILE_IDX = intPreferencesKey("last_smile_idx")
        val LAST_REMINDER = longPreferencesKey("last_reminder_epoch")
        val LAST_DRINK = longPreferencesKey("last_drink_epoch")
        val GLASSES = intPreferencesKey("glasses_today")
        val GLASSES_DATE = stringPreferencesKey("glasses_date")
        val STREAK = intPreferencesKey("streak")
        val LAST_GOAL_DATE = stringPreferencesKey("last_goal_date")
    }

    val flow: Flow<UserPrefs> = store.data.map { p ->
        val today = LocalDate.now()
        val glassesDate = p[K.GLASSES_DATE] ?: ""
        val glasses = if (glassesDate == today.toString()) (p[K.GLASSES] ?: 0) else 0
        val lastGoal = p[K.LAST_GOAL_DATE] ?: ""
        // the streak is only "alive" if the goal was reached today or yesterday
        val streakAlive = lastGoal == today.toString() || lastGoal == today.minusDays(1).toString()
        UserPrefs(
            name = p[K.NAME] ?: "",
            onboardingDone = p[K.ONBOARDED] ?: false,
            startMinutes = p[K.START] ?: (8 * 60),
            endMinutes = p[K.END] ?: (21 * 60),
            intervalMinutes = p[K.INTERVAL] ?: 15,
            debugFast = p[K.DEBUG_FAST] ?: false,
            paused = p[K.PAUSED] ?: false,
            soundOn = p[K.SOUND_ON] ?: true,
            soundChoice = p[K.SOUND_CHOICE] ?: "chime",
            vibrationOn = p[K.VIBRATION] ?: false,
            notifyWhenScreenOn = p[K.NOTIFY_ON] ?: false,
            lastType = p[K.LAST_TYPE] ?: "SMILE",
            lastCharacter = p[K.LAST_CHAR] ?: "",
            lastMessage = p[K.LAST_MSG] ?: "",
            lastWaterIdx = p[K.LAST_WATER_IDX] ?: -1,
            lastSmileIdx = p[K.LAST_SMILE_IDX] ?: -1,
            lastReminderEpoch = p[K.LAST_REMINDER] ?: 0L,
            lastDrinkEpoch = p[K.LAST_DRINK] ?: 0L,
            glassesToday = glasses,
            streak = if (streakAlive) (p[K.STREAK] ?: 0) else 0,
        )
    }

    suspend fun read(): UserPrefs = flow.first()

    suspend fun completeOnboarding(name: String, startMinutes: Int, endMinutes: Int, intervalMinutes: Int) {
        store.edit {
            it[K.NAME] = name.trim()
            it[K.START] = startMinutes
            it[K.END] = endMinutes
            it[K.INTERVAL] = intervalMinutes
            it[K.ONBOARDED] = true
            it[K.LAST_MSG] = "Nice to meet you, ${name.trim()}! 🌈"
            it[K.LAST_CHAR] = "water_drop"
        }
    }

    suspend fun setName(name: String) = store.edit { it[K.NAME] = name.trim() }
    suspend fun setWindow(startMinutes: Int, endMinutes: Int) = store.edit {
        it[K.START] = startMinutes
        it[K.END] = endMinutes
    }

    suspend fun setInterval(minutes: Int) = store.edit { it[K.INTERVAL] = minutes }
    suspend fun setDebugFast(on: Boolean) = store.edit { it[K.DEBUG_FAST] = on }
    suspend fun setPaused(paused: Boolean) = store.edit { it[K.PAUSED] = paused }
    suspend fun setSoundOn(on: Boolean) = store.edit { it[K.SOUND_ON] = on }
    suspend fun setSoundChoice(choice: String) = store.edit { it[K.SOUND_CHOICE] = choice }
    suspend fun setVibration(on: Boolean) = store.edit { it[K.VIBRATION] = on }
    suspend fun setNotifyWhenScreenOn(on: Boolean) = store.edit { it[K.NOTIFY_ON] = on }

    suspend fun recordReminder(type: ReminderType, characterId: String, message: String, messageIdx: Int) {
        store.edit {
            it[K.LAST_TYPE] = type.name
            it[K.LAST_CHAR] = characterId
            it[K.LAST_MSG] = message
            it[K.LAST_REMINDER] = System.currentTimeMillis()
            if (type == ReminderType.WATER) it[K.LAST_WATER_IDX] = messageIdx else it[K.LAST_SMILE_IDX] = messageIdx
        }
    }

    /** Adds one glass. Returns the new total for today. Updates the streak when the daily goal is reached. */
    suspend fun logGlass(): Int {
        var total = 0
        val today = LocalDate.now()
        store.edit {
            val sameDay = (it[K.GLASSES_DATE] ?: "") == today.toString()
            total = (if (sameDay) (it[K.GLASSES] ?: 0) else 0) + 1
            it[K.GLASSES] = total
            it[K.GLASSES_DATE] = today.toString()
            it[K.LAST_DRINK] = System.currentTimeMillis()
            val goal = 8
            val lastGoal = it[K.LAST_GOAL_DATE] ?: ""
            if (total >= goal && lastGoal != today.toString()) {
                val prev = it[K.STREAK] ?: 0
                it[K.STREAK] = if (lastGoal == today.minusDays(1).toString()) prev + 1 else 1
                it[K.LAST_GOAL_DATE] = today.toString()
            }
        }
        return total
    }

    suspend fun resetToday() = store.edit {
        it[K.GLASSES] = 0
        it[K.GLASSES_DATE] = LocalDate.now().toString()
    }
}
