package org.kreal.webdav.sync

import android.app.Application
import android.preference.PreferenceManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        preference = Preference(PreferenceManager.getDefaultSharedPreferences(applicationContext))
    }

    companion object {
        lateinit var preference: Preference
            private set
    }
}