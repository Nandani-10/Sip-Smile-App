package com.sipsmile.app.data

import kotlin.random.Random

enum class ReminderType { WATER, SMILE }

/** All reminder texts. {name} is replaced with the name typed at first launch. */
object Messages {

    val water: List<String> = listOf(
        "Sip time, {name}! 💧",
        "Your body is asking for water, {name} 🥤",
        "Hydration check, {name}! A few sips please 💦",
        "Hey {name}, a glass of water makes your day brighter 🌊",
        "Water break, {name}! Your future self says thanks 💙",
        "Little sip, big smile, {name} 💧",
        "{name}, the water bottle misses you 🫧",
        "Drink up, {name}! Stay fresh like a flower 🌸",
        "Quick sip, {name}, then back to being awesome ✨",
        "Time to refill your energy, {name} 💧",
        "{name}, even clouds drink water. Your turn! ☁️",
        "Splash of water, splash of joy, {name} 🐳",
        "Be kind to your body, {name}. Sip some water 💗",
        "Cheers, {name}! One glass closer to your goal 🥂",
        "Psst {name}... water time! 🤫💧",
        "Stay dewy and refreshed, {name} 🌿💧",
        "A little sip brings a lot of energy, {name}! 💧",
        "Keep your glow flowing, {name}. Take a sip! ✨💧",
        "Cool, clear and refreshing. Sip up, {name}! 💧💙",
        "A happy plant needs water, and so does {name}! 🌱💧",
    )

    val smile: List<String> = listOf(
        "Smile, {name}! The world is brighter when you do 😊",
        "{name}, you are doing better than you think 🌟",
        "Take a breath and smile, {name} 🌼",
        "A tiny smile for a lovely you, {name} 🥰",
        "{name}, today is yours. Smile at it ☀️",
        "Hey {name}, someone is lucky to know you 💛",
        "Smile break, {name}! Even if it's a small one 😄",
        "You've got this, {name}! Show me that smile 😁",
        "{name}, good things are coming. Smile! 🌈",
        "Relax your shoulders and smile, {name} 🦋",
        "{name}, your smile is your superpower 💫",
        "Pause. Smile. Carry on, {name} 🌻",
        "A butterfly just wished you a happy moment, {name} 🦋",
        "Smile like nobody is watching, {name} 🕊️",
        "{name}, you deserve a happy minute right now 💖",
        "Look up from the screen and smile at the sky, {name} ☁️😊",
        "Soft eyes, relaxed jaw, gentle smile. You got this, {name}! 🍃",
        "Notice one lovely thing around you and smile, {name} 🐝",
        "Even the clouds part when {name} smiles 🌤️",
        "Every little smile ripples outward, {name} 🌊😊",
    )

    private fun displayName(name: String): String = name.trim().ifBlank { "friend" }

    fun format(template: String, name: String): String = template.replace("{name}", displayName(name))

    /** Picks a random message that is different from [lastIdx]. Returns (index, text with the name filled in). */
    fun pick(type: ReminderType, name: String, lastIdx: Int): Pair<Int, String> {
        val pool = if (type == ReminderType.WATER) water else smile
        var idx = Random.nextInt(pool.size)
        if (pool.size > 1) {
            while (idx == lastIdx) idx = Random.nextInt(pool.size)
        }
        return idx to format(pool[idx], name)
    }
}
