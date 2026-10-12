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
import java.util.Locale

private val Context.sipDataStore: DataStore<Preferences> by preferencesDataStore(name = "sip_smile_prefs")

/** One day of history. */
data class DayStat(val date: LocalDate, val glasses: Int, val smiles: Int)

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
    val themeMode: String = "light",        // "light", "dark" or "system"
    val accent: String = "sky",             // sky, mint, peach, pink, lavender
    val summaryOn: Boolean = true,          // evening summary notification
    val lastType: String = "COMBO",
    val lastCharacter: String = "",
    val lastMessage: String = "",
    val lastWaterIdx: Int = -1,
    val lastSmileIdx: Int = -1,
    val lastReminderEpoch: Long = 0L,
    val lastDrinkEpoch: Long = 0L,
    val glassesToday: Int = 0,
    val smilesToday: Int = 0,
    val goal: Int = DEFAULT_GOAL,           // glasses per day
    val glassMl: Int = DEFAULT_GLASS_ML,    // size of one glass
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val totalGlasses: Int = 0,
    val totalSmiles: Int = 0,
    val history: List<DayStat> = emptyList(),
) {
    /** The interval actually used for scheduling (1 minute when the testing switch is on). */
    val effectiveInterval: Int get() = if (debugFast) 1 else intervalMinutes

    val goalReached: Boolean get() = goal > 0 && glassesToday >= goal
    val extraGlasses: Int get() = (glassesToday - goal).coerceAtLeast(0)
    val progress: Float get() = if (goal > 0) (glassesToday.toFloat() / goal).coerceIn(0f, 1f) else 0f

    /** "3/8", or "8/8 +2" once the goal is passed (never "10/8"). */
    val progressText: String
        get() = if (glassesToday <= goal) "$glassesToday/$goal" else "$goal/$goal +$extraGlasses"

    val drankMlToday: Int get() = glassesToday * glassMl
    val goalMl: Int get() = goal * glassMl

    companion object {
        const val DEFAULT_GOAL = 8
        const val DEFAULT_GLASS_ML = 250
        const val MIN_GOAL = 1
        const val MAX_GOAL = 30
        val GLASS_SIZES = listOf(150, 200, 250, 300, 500)
    }
}

/** 1500 -> "1.5 L", 250 -> "250 ml". */
fun formatMl(ml: Int): String =
    if (ml >= 1000) String.format(Locale.US, "%.1f L", ml / 1000f) else "$ml ml"

/** Rough hydration guide: about 33 ml per kg of body weight, rounded to whole glasses. */
fun suggestedGlasses(weightKg: Int, glassMl: Int): Int {
    if (weightKg <= 0 || glassMl <= 0) return UserPrefs.DEFAULT_GOAL
    val glasses = Math.round(weightKg * 33f / glassMl)
    return glasses.coerceIn(4, UserPrefs.MAX_GOAL)
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
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val SUMMARY_ON = booleanPreferencesKey("summary_on")
        val LAST_TYPE = stringPreferencesKey("last_type")
        val LAST_CHAR = stringPreferencesKey("last_character")
        val LAST_MSG = stringPreferencesKey("last_message")
        val LAST_WATER_IDX = intPreferencesKey("last_water_idx")
        val LAST_SMILE_IDX = intPreferencesKey("last_smile_idx")
        val LAST_REMINDER = longPreferencesKey("last_reminder_epoch")
        val LAST_DRINK = longPreferencesKey("last_drink_epoch")
        val GOAL = intPreferencesKey("goal_glasses")
        val GLASS_ML = intPreferencesKey("glass_ml")
        // legacy single-day counter from v1.0 (still read once so nobody loses today's glasses)
        val GLASSES = intPreferencesKey("glasses_today")
        val GLASSES_DATE = stringPreferencesKey("glasses_date")
        // "2026-10-12,5,3;2026-10-11,8,6"  = date, glasses, smiles
        val HISTORY = stringPreferencesKey("history")
        val STREAK = intPreferencesKey("streak")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val LAST_GOAL_DATE = stringPreferencesKey("last_goal_date")
        val PREV_GOAL_DATE = stringPreferencesKey("prev_goal_date")
        val PREV_STREAK = intPreferencesKey("prev_streak")
        val TOTAL_GLASSES = intPreferencesKey("total_glasses")
        val TOTAL_SMILES = intPreferencesKey("total_smiles")
    }

    // ---- history helpers -------------------------------------------------------------------

    /** date -> [glasses, smiles] */
    private fun parseHistory(raw: String): MutableMap<LocalDate, IntArray> {
        val map = LinkedHashMap<LocalDate, IntArray>()
        if (raw.isBlank()) return map
        for (part in raw.split(";")) {
            val f = part.split(",")
            if (f.size < 3) continue
            try {
                map[LocalDate.parse(f[0])] = intArrayOf(f[1].toInt(), f[2].toInt())
            } catch (e: Exception) {
                // skip a damaged entry
            }
        }
        return map
    }

    private fun writeHistory(map: Map<LocalDate, IntArray>): String =
        map.entries
            .sortedByDescending { it.key }
            .take(120)
            .joinToString(";") { "${it.key},${it.value[0]},${it.value[1]}" }

    private fun legacyGlasses(p: Preferences, today: LocalDate): Int =
        if ((p[K.GLASSES_DATE] ?: "") == today.toString()) (p[K.GLASSES] ?: 0) else 0

    // ---- reading ---------------------------------------------------------------------------

    val flow: Flow<UserPrefs> = store.data.map { p ->
        val today = LocalDate.now()
        val hist = parseHistory(p[K.HISTORY] ?: "")
        val todayEntry = hist[today]
        val glasses = todayEntry?.get(0) ?: legacyGlasses(p, today)
        val smiles = todayEntry?.get(1) ?: 0
        val lastGoal = p[K.LAST_GOAL_DATE] ?: ""
        // the streak is only "alive" if the goal was reached today or yesterday
        val streakAlive = lastGoal == today.toString() || lastGoal == today.minusDays(1).toString()
        val streak = if (streakAlive) (p[K.STREAK] ?: 0) else 0
        val days = hist.entries
            .map { DayStat(it.key, it.value[0], it.value[1]) }
            .sortedBy { it.date }
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
            themeMode = p[K.THEME] ?: "light",
            accent = p[K.ACCENT] ?: "sky",
            summaryOn = p[K.SUMMARY_ON] ?: true,
            lastType = p[K.LAST_TYPE] ?: "COMBO",
            lastCharacter = p[K.LAST_CHAR] ?: "",
            lastMessage = p[K.LAST_MSG] ?: "",
            lastWaterIdx = p[K.LAST_WATER_IDX] ?: -1,
            lastSmileIdx = p[K.LAST_SMILE_IDX] ?: -1,
            lastReminderEpoch = p[K.LAST_REMINDER] ?: 0L,
            lastDrinkEpoch = p[K.LAST_DRINK] ?: 0L,
            glassesToday = glasses,
            smilesToday = smiles,
            goal = (p[K.GOAL] ?: UserPrefs.DEFAULT_GOAL).coerceIn(UserPrefs.MIN_GOAL, UserPrefs.MAX_GOAL),
            glassMl = p[K.GLASS_ML] ?: UserPrefs.DEFAULT_GLASS_ML,
            streak = streak,
            bestStreak = maxOf(p[K.BEST_STREAK] ?: 0, streak),
            totalGlasses = p[K.TOTAL_GLASSES] ?: 0,
            totalSmiles = p[K.TOTAL_SMILES] ?: 0,
            history = days,
        )
    }

    suspend fun read(): UserPrefs = flow.first()

    // ---- simple setters --------------------------------------------------------------------

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
    suspend fun setThemeMode(mode: String) = store.edit { it[K.THEME] = mode }
    suspend fun setAccent(accent: String) = store.edit { it[K.ACCENT] = accent }
    suspend fun setSummaryOn(on: Boolean) = store.edit { it[K.SUMMARY_ON] = on }
    suspend fun setGoal(glasses: Int) = store.edit {
        it[K.GOAL] = glasses.coerceIn(UserPrefs.MIN_GOAL, UserPrefs.MAX_GOAL)
    }

    suspend fun setGlassMl(ml: Int) = store.edit { it[K.GLASS_ML] = ml }

    suspend fun recordReminder(type: ReminderType, characterId: String, message: String, messageIdx: Int) {
        store.edit {
            it[K.LAST_TYPE] = type.name
            it[K.LAST_CHAR] = characterId
            it[K.LAST_MSG] = message
            it[K.LAST_REMINDER] = System.currentTimeMillis()
            if (type == ReminderType.WATER) it[K.LAST_WATER_IDX] = messageIdx else it[K.LAST_SMILE_IDX] = messageIdx
        }
    }

    // ---- water + smiles --------------------------------------------------------------------

    /** Adds one glass. Returns the new total for today. Updates the streak when the daily goal is reached. */
    suspend fun logGlass(): Int {
        var total = 0
        val today = LocalDate.now()
        store.edit { p ->
            val hist = parseHistory(p[K.HISTORY] ?: "")
            val entry = hist.getOrPut(today) { intArrayOf(legacyGlasses(p, today), 0) }
            entry[0] += 1
            total = entry[0]
            p[K.HISTORY] = writeHistory(hist)
            p[K.TOTAL_GLASSES] = (p[K.TOTAL_GLASSES] ?: 0) + 1
            p[K.LAST_DRINK] = System.currentTimeMillis()

            val goal = (p[K.GOAL] ?: UserPrefs.DEFAULT_GOAL)
            val lastGoal = p[K.LAST_GOAL_DATE] ?: ""
            if (total >= goal && lastGoal != today.toString()) {
                val prev = p[K.STREAK] ?: 0
                val continues = lastGoal == today.minusDays(1).toString()
                val newStreak = if (continues) prev + 1 else 1
                p[K.PREV_GOAL_DATE] = lastGoal
                p[K.PREV_STREAK] = prev
                p[K.STREAK] = newStreak
                p[K.LAST_GOAL_DATE] = today.toString()
                if (newStreak > (p[K.BEST_STREAK] ?: 0)) p[K.BEST_STREAK] = newStreak
            }
        }
        return total
    }

    /** Takes back the last glass (for an accidental tap). Returns the new total for today. */
    suspend fun undoGlass(): Int {
        var total = 0
        val today = LocalDate.now()
        store.edit { p ->
            val hist = parseHistory(p[K.HISTORY] ?: "")
            val entry = hist.getOrPut(today) { intArrayOf(legacyGlasses(p, today), 0) }
            if (entry[0] > 0) {
                entry[0] -= 1
                p[K.TOTAL_GLASSES] = ((p[K.TOTAL_GLASSES] ?: 0) - 1).coerceAtLeast(0)
            }
            total = entry[0]
            p[K.HISTORY] = writeHistory(hist)

            val goal = (p[K.GOAL] ?: UserPrefs.DEFAULT_GOAL)
            if (total < goal && (p[K.LAST_GOAL_DATE] ?: "") == today.toString()) {
                // the goal is no longer reached today: give the streak back
                p[K.STREAK] = p[K.PREV_STREAK] ?: 0
                p[K.LAST_GOAL_DATE] = p[K.PREV_GOAL_DATE] ?: ""
            }
        }
        return total
    }

    /** Counts one smile. Returns today's smile total. */
    suspend fun logSmile(): Int {
        var total = 0
        val today = LocalDate.now()
        store.edit { p ->
            val hist = parseHistory(p[K.HISTORY] ?: "")
            val entry = hist.getOrPut(today) { intArrayOf(legacyGlasses(p, today), 0) }
            entry[1] += 1
            total = entry[1]
            p[K.HISTORY] = writeHistory(hist)
            p[K.TOTAL_SMILES] = (p[K.TOTAL_SMILES] ?: 0) + 1
        }
        return total
    }

    suspend fun resetToday() = store.edit { p ->
        val today = LocalDate.now()
        val hist = parseHistory(p[K.HISTORY] ?: "")
        val old = hist[today]?.get(0) ?: legacyGlasses(p, today)
        val smiles = hist[today]?.get(1) ?: 0
        hist[today] = intArrayOf(0, smiles)
        p[K.HISTORY] = writeHistory(hist)
        p[K.TOTAL_GLASSES] = ((p[K.TOTAL_GLASSES] ?: 0) - old).coerceAtLeast(0)
        p[K.GLASSES] = 0
        p[K.GLASSES_DATE] = today.toString()
        if ((p[K.LAST_GOAL_DATE] ?: "") == today.toString()) {
            p[K.STREAK] = p[K.PREV_STREAK] ?: 0
            p[K.LAST_GOAL_DATE] = p[K.PREV_GOAL_DATE] ?: ""
        }
    }
}
