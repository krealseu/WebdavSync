package org.kreal.webdav.sync

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.os.Bundle
import android.provider.CallLog
import android.provider.UserDictionary
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import org.kreal.permissiongrant.PermissionGrant
import org.kreal.webdav.sync.syncadapter.data.DataHelp
import org.kreal.webdav.sync.backup.BackupManager
import org.kreal.webdav.sync.backup.IBackupManager
import org.kreal.webdav.sync.backup.WebDavStorage

class MainActivity : AppCompatActivity(), View.OnClickListener {

    override fun onClick(v: View) {
        when (v.id) {
            R.id.backupCallLog -> accounts.forEach { ContentResolver.requestSync(it, CallLog.AUTHORITY, Bundle.EMPTY) }
            R.id.backupDictionary -> accounts.forEach { ContentResolver.requestSync(it, UserDictionary.AUTHORITY, Bundle.EMPTY) }
            R.id.backupSms -> accounts.forEach { ContentResolver.requestSync(it, "sms", Bundle.EMPTY) }
            R.id.restoreDictionary -> {
                Thread {
                    val accountManager = AccountManager.get(applicationContext)
                    val backup = BackupManager(WebDavStorage(accounts[0].name, accountManager.getPassword(accounts[0]), accountManager.getUserData(accounts[0], Constants.ACCOUNT_SERVER) + "/${App.preference.phoneName}"))
                    val nodes = backup.listFile(Preference.dictionaryKEY)
                    var recentNode: IBackupManager.Node? = null
                    nodes.forEach {
                        if (recentNode == null || recentNode!!.date < it.date)
                            recentNode = it
                    }
                    recentNode?.also {
                        val json = backup.getKey(it)
                                ?: return@Thread
                        DataHelp.restoreDictionary(this, DataHelp.loadDictionaries(json))
                    }
                }.start()
            }
            R.id.restoreCallLog -> {
                Thread {
                    val accountManager = AccountManager.get(applicationContext)
                    val backup = BackupManager(WebDavStorage(accounts[0].name, accountManager.getPassword(accounts[0]), accountManager.getUserData(accounts[0], Constants.ACCOUNT_SERVER) + "/${App.preference.phoneName}"))
                    val nodes = backup.listFile(Preference.callKEY)
                    var recentNode: IBackupManager.Node? = null
                    nodes.forEach {
                        if (recentNode == null || recentNode!!.date < it.date)
                            recentNode = it
                    }
                    recentNode?.also {
                        val json = backup.getKey(it)
                                ?: return@Thread
                        DataHelp.restoreCallLogs(this, DataHelp.loadCallLogs(json))
                    }
                }.start()
            }
            R.id.restoreSms -> {
                Thread {
                    val accountManager = AccountManager.get(applicationContext)
                    val backup = BackupManager(WebDavStorage(accounts[0].name, accountManager.getPassword(accounts[0]), accountManager.getUserData(accounts[0], Constants.ACCOUNT_SERVER) + "/${App.preference.phoneName}"))
                    val nodes = backup.listFile(Preference.smsKEY)
                    var recentNode: IBackupManager.Node? = null
                    nodes.forEach {
                        if (recentNode == null || recentNode!!.date < it.date)
                            recentNode = it
                    }
                    recentNode?.also {
                        val json = backup.getKey(it)
                                ?: return@Thread
                        DataHelp.loadSmsInfos(json)
                    }
                }.start()
            }
        }
    }

    private lateinit var accounts: Array<Account>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        accounts = AccountManager.get(applicationContext).getAccountsByType(Constants.ACCOUNT_TYPE)
        accounts.forEach {
            ContentResolver.setIsSyncable(it, CallLog.AUTHORITY, 1)
            ContentResolver.setIsSyncable(it, UserDictionary.AUTHORITY, 1)
            ContentResolver.setIsSyncable(it, "sms", 1)
            ContentResolver.addPeriodicSync(it, CallLog.AUTHORITY, Bundle.EMPTY, 3600 * 24)
            ContentResolver.addPeriodicSync(it, UserDictionary.AUTHORITY, Bundle.EMPTY, 3600 * 24)
            ContentResolver.addPeriodicSync(it, "sms", Bundle.EMPTY, 3600 * 24)
        }
        backupSms.setOnClickListener(this)
        backupCallLog.setOnClickListener(this)
        backupDictionary.setOnClickListener(this)
        restoreDictionary.setOnClickListener(this)
        restoreCallLog.setOnClickListener(this)
        restoreSms.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        sms_sync_time.text = "Last sync time:${App.preference.getKeyTime(key = Preference.smsKEY)}"
        call_sync_time.text = "Last sync time:${App.preference.getKeyTime(key = Preference.callKEY)}"
        dictionary_sync_time.text = "Last sync time:${App.preference.getKeyTime(key = Preference.dictionaryKEY)}"
    }

    class MyPermissionGrant : PermissionGrant() {
        override val permissions: Array<String> = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG
        )
    }
}
