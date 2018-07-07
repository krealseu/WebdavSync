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
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import com.google.gson.Gson
import org.kreal.webdav.sync.utils.Backup
import org.kreal.webdav.sync.utils.WebDav
import java.io.IOException


class CallLogSyncService : AbstractSyncService() {
    override fun getAdapter(): AbstractThreadedSyncAdapter = CallLogSyncAdapter(applicationContext, true)

    class CallLogSyncAdapter(mContext: Context, autoInitialize: Boolean) : AbstractThreadedSyncAdapter(mContext, autoInitialize) {
        override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
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
            val c = provider.query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DEFAULT_SORT_ORDER)
            val callLogs: MutableList<CallLogInfo> = arrayListOf()
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
                callLogs.add(CallLogInfo(number, postDialDigits, viaNumber, numberPresentation, type, features, date, duration, dataUsage))
            }
            c.close()
            val accountManager = AccountManager.get(context)
            try {
                val backup = Backup(WebDav(account.name, accountManager.getPassword(account), accountManager.getUserData(account, Context.ACCOUNT_SERVICE) + "/phone"))
                backup.backup("call", Gson().toJson(callLogs))
            } catch (e: IOException) {
                syncResult.stats.numIoExceptions++
            } catch (e: Exception) {
                syncResult.stats.numUpdates++
            }
        }

    }

    data class CallLogInfo(
            val number: String,
            val postDialDigits: String = "",
            val viaNumber: String = "",
            val numberPresentation: Int,
            val callType: Int,
            val features: Int,
            val date: Long,
            val duration: Long,
            val dataUsage: Long?
    )
}
