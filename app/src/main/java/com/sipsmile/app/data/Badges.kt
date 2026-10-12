package com.sipsmile.app.data

/** A little achievement. Everything is worked out from the saved totals, nothing extra is stored. */
data class Badge(val emoji: String, val title: String, val hint: String, val unlocked: Boolean)

object Badges {
    fun evaluate(p: UserPrefs, photoCount: Int): List<Badge> = listOf(
        Badge("💧", "First Sip", "Log your first glass", p.totalGlasses >= 1),
        Badge("🎯", "Goal Getter", "Reach your daily goal once", p.bestStreak >= 1),
        Badge("🔥", "3-Day Streak", "Reach your goal 3 days in a row", p.bestStreak >= 3),
        Badge("🏆", "Week Warrior", "7 days in a row", p.bestStreak >= 7),
        Badge("👑", "Hydration Royalty", "14 days in a row", p.bestStreak >= 14),
        Badge("🌊", "100 Sips", "Drink 100 glasses in total", p.totalGlasses >= 100),
        Badge("🐳", "500 Sips", "Drink 500 glasses in total", p.totalGlasses >= 500),
        Badge("😊", "Smile Starter", "Log your first smile", p.totalSmiles >= 1),
        Badge("🌻", "Smile Collector", "Smile 50 times", p.totalSmiles >= 50),
        Badge("🌈", "Happy Day", "Reach the goal and smile 3 times in one day", p.goalReached && p.smilesToday >= 3),
        Badge("📸", "Say Cheese", "Capture your first smile", photoCount >= 1),
        Badge("🖼️", "Smile Album", "Capture 10 smiles", photoCount >= 10),
    )
}
