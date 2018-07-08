package org.kreal.webdav.sync.syncadapter

import android.accounts.Account
import android.accounts.AccountManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import com.google.gson.Gson
import org.kreal.webdav.sync.App
import org.kreal.webdav.sync.Constants
import org.kreal.webdav.sync.Preference
import org.kreal.webdav.sync.syncadapter.data.DataHelp
import org.kreal.webdav.sync.backup.BackupManager
import org.kreal.webdav.sync.backup.WebDavStorage

class SmsSyncService : AbstractSyncService() {
    override fun getAdapter(): AbstractThreadedSyncAdapter = SmsSyncAdapter(applicationContext, true)

    class SmsSyncAdapter(context: Context, autoInitialize: Boolean) : AbstractThreadedSyncAdapter(context, autoInitialize) {
        override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
            val accountManager = AccountManager.get(context)
            val smsInfosJson = Gson().toJson(DataHelp.loadSmsInfos(context))
            val backup = BackupManager(WebDavStorage(account.name, accountManager.getPassword(account), accountManager.getUserData(account, Constants.ACCOUNT_SERVER) + "/${App.preference.phoneName}"))
            if (backup.backup("sms", smsInfosJson))
                App.preference.recordKeyTime(Preference.smsKEY)
            else syncResult.stats.numIoExceptions++
        }
    }
}
