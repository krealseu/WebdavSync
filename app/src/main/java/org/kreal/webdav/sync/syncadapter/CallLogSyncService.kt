/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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


class CallLogSyncService : AbstractSyncService() {
    override fun getAdapter(): AbstractThreadedSyncAdapter = CallLogSyncAdapter(applicationContext, true)

    class CallLogSyncAdapter(mContext: Context, autoInitialize: Boolean) : AbstractThreadedSyncAdapter(mContext, autoInitialize) {
        override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
            val accountManager = AccountManager.get(context)
            val backup = BackupManager(WebDavStorage(account.name, accountManager.getPassword(account), accountManager.getUserData(account, Constants.ACCOUNT_SERVER) + "/${App.preference.phoneName}"))
            val callLogJson = Gson().toJson(DataHelp.loadCallLogs(context))
            if (backup.backup("call", callLogJson))
                App.preference.recordKeyTime(Preference.callKEY)
            else syncResult.stats.numIoExceptions++
        }

    }
}
