package com.sipsmile.app

/** Tiny in-memory flag: is the app on screen right now? (set by MainActivity) */
object AppState {
    @Volatile
    var foreground: Boolean = false
}
