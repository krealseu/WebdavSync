package org.kreal.webdav.sync.syncadapter

import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.Intent
import android.os.IBinder

/**
 * Service to handle Account sync. This is invoked with an intent with action
 * ACTION_AUTHENTICATOR_INTENT. It instantiates the AbstractThreadedSyncAdapter and returns its
 * IBinder.
 */
abstract class AbstractSyncService : Service() {
    private var sSyncAdapter: AbstractThreadedSyncAdapter? = null

    override fun onCreate() {
        synchronized(sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = getAdapter()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return sSyncAdapter!!.syncAdapterBinder
    }

    abstract fun getAdapter(): AbstractThreadedSyncAdapter

    companion object {
        private val sSyncAdapterLock = Any()
    }
}