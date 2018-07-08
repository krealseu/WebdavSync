package org.kreal.webdav.sync.syncadapter.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import android.provider.Telephony
import android.provider.UserDictionary
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.util.*

object DataHelp {

    fun loadSmsInfos(context: Context): List<SmsInfo> {
        val result = arrayListOf<SmsInfo>()
        val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.DATE,
                Telephony.Sms.SUBJECT,
                Telephony.Sms.READ,
                Telephony.Sms.DATE_SENT,
                Telephony.Sms.TYPE,
                Telephony.Sms.BODY
        )
        if (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED else true) {
            val c = context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, null, null, Telephony.Sms.DEFAULT_SORT_ORDER)
            while (c.moveToNext()) {
                val address = c.getString(c.getColumnIndex(Telephony.Sms.ADDRESS)) ?: continue
                val body = c.getString(c.getColumnIndex(Telephony.Sms.BODY))
                val subject = c.getString(c.getColumnIndex(Telephony.Sms.SUBJECT))
                val date = c.getLong(c.getColumnIndex(Telephony.Sms.DATE))
                val dateSend = c.getLong(c.getColumnIndex(Telephony.Sms.DATE_SENT))
                val read = c.getInt(c.getColumnIndex(Telephony.Sms.READ))
                val type = c.getInt(c.getColumnIndex(Telephony.Sms.TYPE))
                result.add(SmsInfo(address, body, subject, date, dateSend, read, type))
            }
            c.close()
        }
        return result
    }

    fun loadSmsInfos(json: String): List<SmsInfo> = parserJsonArrayList(json)

    fun loadCallLogs(context: Context): List<CallLogInfo> {
        val result = arrayListOf<CallLogInfo>()
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            arrayOf(CallLog.Calls.TYPE, CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE, CallLog.Calls.DURATION,
                    CallLog.Calls.DATA_USAGE, CallLog.Calls.VIA_NUMBER,
                    CallLog.Calls.NUMBER_PRESENTATION, CallLog.Calls.NUMBER_PRESENTATION,
                    CallLog.Calls.POST_DIAL_DIGITS, CallLog.Calls.FEATURES

            )
        } else {
            arrayOf(CallLog.Calls.TYPE, CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE, CallLog.Calls.DURATION,
                    CallLog.Calls.DATA_USAGE, CallLog.Calls.NUMBER_PRESENTATION,
                    CallLog.Calls.NUMBER_PRESENTATION, CallLog.Calls.FEATURES
            )
        }

        if (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED else true) {
            val c = context.contentResolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DEFAULT_SORT_ORDER)
            while (c.moveToNext()) {
                val type = c.getInt(c.getColumnIndex(CallLog.Calls.TYPE))
                val number = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER))
                val date = c.getLong(c.getColumnIndex(CallLog.Calls.DATE))
                val duration = c.getLong(c.getColumnIndex(CallLog.Calls.DURATION))
                val dataUsage = c.getLong(c.getColumnIndex(CallLog.Calls.DATA_USAGE))
                val viaNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) c.getString(c.getColumnIndex(CallLog.Calls.VIA_NUMBER)) else ""
                val numberPresentation = c.getInt(c.getColumnIndex(CallLog.Calls.NUMBER_PRESENTATION))
                val postDialDigits = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) c.getString(c.getColumnIndex(CallLog.Calls.POST_DIAL_DIGITS)) else ""
                val features = c.getInt(c.getColumnIndex(CallLog.Calls.FEATURES))
                result.add(CallLogInfo(number, postDialDigits, viaNumber, numberPresentation, type, features, date, duration, dataUsage))
            }
            c.close()
        }
        return result
    }

    fun loadCallLogs(json: String): List<CallLogInfo> = parserJsonArrayList(json)

    fun loadDictionaries(context: Context): List<Word> {
        val projection: Array<String> = arrayOf(UserDictionary.Words._ID, UserDictionary.Words.WORD, UserDictionary.Words.SHORTCUT, UserDictionary.Words.LOCALE, UserDictionary.Words.FREQUENCY)
        val cursor = context.contentResolver.query(UserDictionary.Words.CONTENT_URI, projection, null, null, null)
        val result: MutableList<Word> = ArrayList(3)
        while (cursor.moveToNext()) {
            result.add(Word(id = cursor.getInt(cursor.getColumnIndex(UserDictionary.Words._ID)),
                    word = cursor.getString(cursor.getColumnIndex(UserDictionary.Words.WORD)),
                    shortcut = cursor.getString(cursor.getColumnIndex(UserDictionary.Words.SHORTCUT))
                            ?: "",
                    frequency = cursor.getInt(cursor.getColumnIndex(UserDictionary.Words.FREQUENCY)),
                    locale = cursor.getString(cursor.getColumnIndex(UserDictionary.Words.LOCALE))))
        }
        cursor.close()
        return result
    }

    fun loadDictionaries(json: String): List<Word> = parserJsonArrayList(json)

    fun restoreCallLogs(context: Context, callLogInfos: List<CallLogInfo>): Int {
        if (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_DENIED else false)
            return 0
        val exist = loadCallLogs(context)
        val newValues = arrayListOf<ContentValues>()
        callLogInfos.forEach {
            if (!exist.contains(element = it)) {
                val values = ContentValues()
                values.apply {
                    put(CallLog.Calls.NUMBER, it.number)
                    put(CallLog.Calls.NUMBER_PRESENTATION, it.numberPresentation)
                    put(CallLog.Calls.TYPE, it.callType)
                    put(CallLog.Calls.FEATURES, it.features)
                    put(CallLog.Calls.DATE, it.date)
                    put(CallLog.Calls.DURATION, it.duration)
                    put(CallLog.Calls.DATA_USAGE, it.dataUsage)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        put(CallLog.Calls.POST_DIAL_DIGITS, it.postDialDigits)
                        put(CallLog.Calls.VIA_NUMBER, it.viaNumber)
                    }
                }
                newValues.add(values)
            }
        }
        return context.contentResolver.bulkInsert(CallLog.Calls.CONTENT_URI, newValues.toTypedArray())
    }

    fun restoreSms(context: Context, smsInfos: List<SmsInfo>): Int {
        if (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED else false)
            return 0
        val exist = loadSmsInfos(context)
        val newValues = arrayListOf<ContentValues>()
        smsInfos.forEach {
            if (!exist.contains(it)) {
                val values = ContentValues()
                values.apply {
                    put(Telephony.Sms.ADDRESS, it.address)
                    put(Telephony.Sms.BODY, it.body)
                    if (it.subject != null)
                        put(Telephony.Sms.SUBJECT, it.subject)
                    put(Telephony.Sms.DATE, it.date)
                    put(Telephony.Sms.DATE_SENT, it.dateSend)
                    put(Telephony.Sms.READ, it.read)
                    put(Telephony.Sms.TYPE, it.type)
                }
                newValues.add(values)
            }
        }
        return context.contentResolver.bulkInsert(Telephony.Sms.CONTENT_URI, newValues.toTypedArray())
    }

    fun restoreDictionary(context: Context, words: List<Word>): Int {
        val exist = loadDictionaries(context)
        val newValues = arrayListOf<ContentValues>()
        words.forEach { word ->
            if (exist.find { it.word == word.word && it.shortcut == word.shortcut && it.locale == word.locale } == null) {
                val values = ContentValues()
                values.apply {
                    put(UserDictionary.Words.LOCALE, word.locale)
                    put(UserDictionary.Words.WORD, word.word)
                    put(UserDictionary.Words.SHORTCUT, word.shortcut)
                    put(UserDictionary.Words.FREQUENCY, word.frequency)
                }
                newValues.add(values)
            }
        }
        return context.contentResolver.bulkInsert(UserDictionary.Words.CONTENT_URI, newValues.toTypedArray())
    }

    private inline fun <reified T> parserJsonArrayList(json: String): List<T> {
        val parser = JsonParser()
        val jsonArray = parser.parse(json).asJsonArray
        val result = arrayListOf<T>()
        val gson = Gson()
        jsonArray.forEach {
            result.add(gson.fromJson(it, T::class.java))
        }
        return result
    }
}