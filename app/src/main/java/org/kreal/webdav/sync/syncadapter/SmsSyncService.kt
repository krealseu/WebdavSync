package org.kreal.webdav.sync.syncadapter

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import com.google.gson.Gson
import org.kreal.webdav.sync.utils.Backup
import org.kreal.webdav.sync.utils.WebDav
import java.io.IOException

class SmsSyncService : AbstractSyncService() {
    override fun getAdapter(): AbstractThreadedSyncAdapter = SmsSyncAdapter(applicationContext, true)

    class SmsSyncAdapter(context: Context, autoInitialize: Boolean) : AbstractThreadedSyncAdapter(context, autoInitialize) {
        override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
            val smsInfos = arrayListOf<SmsInfo>()
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
                val c = provider.query(Telephony.Sms.CONTENT_URI, projection, null, null, Telephony.Sms.DEFAULT_SORT_ORDER)
                while (c.moveToNext()) {
                    val address = c.getString(c.getColumnIndex(Telephony.Sms.ADDRESS)) ?: continue
                    val body = c.getString(c.getColumnIndex(Telephony.Sms.BODY))
                    val subject = c.getString(c.getColumnIndex(Telephony.Sms.SUBJECT))
                    val date = c.getLong(c.getColumnIndex(Telephony.Sms.DATE))
                    val dateSend = c.getLong(c.getColumnIndex(Telephony.Sms.DATE_SENT))
                    val read = c.getInt(c.getColumnIndex(Telephony.Sms.READ))
                    val type = c.getInt(c.getColumnIndex(Telephony.Sms.TYPE))
                    smsInfos.add(SmsInfo(address, body, subject, date, dateSend, read, type))
                }
                c.close()
            }
            val accountManager = AccountManager.get(context)
            try {
                val backup = Backup(WebDav(account.name, accountManager.getPassword(account), accountManager.getUserData(account, Context.ACCOUNT_SERVICE) + "/phone"))
                backup.backup("sms", Gson().toJson(smsInfos))
            } catch (e: IOException) {
                syncResult.stats.numIoExceptions++
            } catch (e: Exception) {
                syncResult.stats.numUpdates++
            }
        }
    }

    /**
     * @param address 电话号码
     * @param date 短信接受
     * @param dateSend 短信发送时间
     * @param type 短信类型
     * @param body 短信类容
     */
    data class SmsInfo(
            val address: String,
            val body: String,
            val subject: String?,
            val date: Long,
            val dateSend: Long,
            val read: Int,
            val type: Int
    )
}
