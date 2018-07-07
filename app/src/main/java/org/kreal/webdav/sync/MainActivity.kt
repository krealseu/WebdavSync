package org.kreal.webdav.sync

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import org.kreal.permissiongrant.PermissionGrant
import org.kreal.webdav.sync.utils.Backup
import org.kreal.webdav.sync.utils.WebDav

class MainActivity : AppCompatActivity(), PermissionGrant.PermissionGrantListener {
    override fun onReject() {

    }

    override fun onGrant() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val permissionGrant = MyPermissionGrant()
        if (!permissionGrant.checkPermissions(baseContext))
            permissionGrant.show(fragmentManager, "storage")
        Log.i("afd", Build.MODEL)
        Thread {
            val dav = WebDav("lthee12@hotmail.com", "axdrkyzsc66usbtw", "https://dav.jianguoyun.com/dav/Backup/test")
            val bb = Backup(dav)
            bb.backup("sms", "asdfjkakkkshdfkjash000dfkjhakjsdfhkajsdf")

            Log.i("afd", bb.restore("sms"))
            Log.i("afd",2345.toByte().toString())
            dav.list().forEach {
                Log.i("afd", it)
            }
        }.start()
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
