package com.sipsmile.app

import android.app.Application
import com.sipsmile.app.reminder.Notifier

class SipSmileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // notification channels must exist before the first notification is posted
        Notifier.createChannels(this)
    }
}
