package org.kreal.webdav.sync.utils

import android.os.Build
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class Backup(private val webDav: WebDav) {

    fun backup(key: String, value: String) {
        val hashCode = value.hashCode().toString()
        val name = webDav.list()
        val regex = createRegex(hashCode, key)
        name.forEach {
            if (it.matches(regex)) {
                webDav.rename(it, createName(hashCode, key))
                return
            }
        }
        val valueBytes = ZLibUtils.compress(value.toByteArray())
        exchangeBytes(valueBytes)
        webDav.upLoad(createName(hashCode, key), ByteArrayInputStream(valueBytes))
    }

    fun backupFile(name: String, inputStream: InputStream) = webDav.upLoad(name, inputStream)

    fun restore(key: String): String {
        val name = webDav.list()
        name.forEach {
            if (it.contains(key)) {
                val valueBytes = webDav.download(it).readBytes()
                exchangeBytes(valueBytes)
                return String(ZLibUtils.uncompress(valueBytes))
            }
        }
        return ""
    }

    private fun exchangeBytes(array: ByteArray) {
        val len = array.size
        for (i in 0..(len - 1) / 2 step 2) {
            val tmp = array[i]
            array[i] = array[len - 1 - i]
            array[len - 1 - i] = tmp
        }
    }

    //nameFormat Model-key-yyyyMMddHHmm-hashCode
    private fun createName(hashCode: String, key: String) = "${Build.MODEL}-$key-${getDataString()}-$hashCode"

    //match name
    private fun createRegex(hashCode: String, key: String) = "^${Build.MODEL}-$key-\\d{12}-$hashCode$".toRegex()

    private fun getDataString() = SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA).format(Date(System.currentTimeMillis()))

}