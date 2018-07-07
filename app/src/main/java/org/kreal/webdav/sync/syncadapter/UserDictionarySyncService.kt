package org.kreal.webdav.sync.syncadapter

import android.accounts.Account
import android.accounts.AccountManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import android.provider.UserDictionary
import com.google.gson.Gson
import org.kreal.webdav.sync.utils.Backup
import org.kreal.webdav.sync.utils.WebDav
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

class UserDictionarySyncService : AbstractSyncService() {

    override fun getAdapter(): AbstractThreadedSyncAdapter = UserDictionarySyncAdapter(applicationContext, true)

    class UserDictionarySyncAdapter(context: Context, autoInitialize: Boolean) : AbstractThreadedSyncAdapter(context, autoInitialize) {

        override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
            val projection: Array<String> = arrayOf(UserDictionary.Words._ID, UserDictionary.Words.WORD, UserDictionary.Words.SHORTCUT, UserDictionary.Words.LOCALE, UserDictionary.Words.FREQUENCY)
            val cursor = provider.query(UserDictionary.Words.CONTENT_URI, projection, null, null, null)
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
            val accountManager = AccountManager.get(context)
            try {
                val backup = Backup(WebDav(account.name, accountManager.getPassword(account), accountManager.getUserData(account, Context.ACCOUNT_SERVICE) + "/phone"))
                backup.backup("dictionary", Gson().toJson(result))
                backup.backupFile("user-dictionary.txt", FileInputStream(File("/sdcard/GooglePinyinInput/user-dictionary.txt")))
            } catch (e: IOException) {
                syncResult.stats.numIoExceptions++
            } catch (e: Exception) {
                syncResult.stats.numUpdates++
            }
        }
    }

    data class Word(val word: String, val shortcut: String, val id: Int = 0, val locale: String? = null, val frequency: Int = 1)
}
