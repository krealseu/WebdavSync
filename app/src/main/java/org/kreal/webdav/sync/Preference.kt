package org.kreal.webdav.sync

import android.content.SharedPreferences
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

class Preference(private val sharedPreferences: SharedPreferences) {

    fun getKeyTime(key: String) = sharedPreferences.getString(key, "None")

    fun recordKeyTime(key: String) = sharedPreferences.edit().putString(key, getDataString()).commit()

    val phoneName = Build.MODEL

    private fun getDataString() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(System.currentTimeMillis()))

    companion object {
        const val smsKEY = "sms"

        const val callKEY = "call"

        const val dictionaryKEY = "dictionary"
    }
}