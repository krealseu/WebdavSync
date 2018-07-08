package org.kreal.webdav.sync.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.Deflater
import java.util.zip.Inflater

class BackupManager(private val storage: IStoraage) : IBackupManager {

    override fun backup(key: String, info: String): Boolean {
        val node = IBackupManager.Node(key, getDateString(), String.format("%011d", info.hashCode()))
        val names = storage.list()
        names.forEach {
            if (node.match(it) && storage.rename(it, node.toName()))
                return true
        }
        val infoBytes = info.toByteArray()
        val compressBytes = zip.compress(infoBytes)
        exchangeBytes(compressBytes)
        return storage.upload(node.toName(), ByteArrayInputStream(compressBytes))
    }

    override fun listKey(key: String): Array<IBackupManager.Node> {
        val result: MutableList<IBackupManager.Node> = arrayListOf()
        val names = storage.list()
        names.forEach {
            if (it.containKey(key))
                it.toNode()?.also {
                    result += it
                }
        }
        return result.toTypedArray()
    }

    override fun getKey(node: IBackupManager.Node): String? {
        val names = storage.list()
        val nodeName = node.toName()
        val findName = names.find { it == nodeName } ?: return null
        val readBytes = storage.download(findName)?.readBytes() ?: return null
        exchangeBytes(readBytes)
        return String(zip.uncompress(readBytes))
    }


    override fun backupFile(name: String, inputStream: InputStream): Boolean {
        val node = IBackupManager.Node(name, getDateString(), String.format("%011d", name.hashCode()))
        val names = storage.list()
        names.forEach {
            if (node.match(it) && storage.rename(it, node.toName()))
                return true
        }
        return storage.upload(node.toName(), inputStream)
    }

    override fun listFile(name: String): Array<IBackupManager.Node> {
        val result: MutableList<IBackupManager.Node> = arrayListOf()
        val names = storage.list()
        names.forEach {
            if (it.containKey(name))
                it.toNode()?.also {
                    result += it
                }
        }
        return result.toTypedArray()
    }

    override fun getFile(node: IBackupManager.Node): InputStream? {
        val names = storage.list()
        val nodeName = node.toName()
        val findName = names.find { it == nodeName } ?: return null
        return storage.download(findName)
    }

    private fun exchangeBytes(array: ByteArray) {
        val len = array.size
        for (i in 0..(len - 1) / 2 step 2) {
            val tmp = array[i]
            array[i] = array[len - 1 - i]
            array[len - 1 - i] = tmp
        }
    }

    private val zip = object {

        private val bytesLength = 1024

        fun compress(byteArray: ByteArray): ByteArray {
            val deflate = Deflater()
            deflate.reset()
            deflate.setInput(byteArray)
            deflate.finish()
            val outputStream = ByteArrayOutputStream()
            val bytes = ByteArray(bytesLength)
            while (!deflate.finished()) outputStream.write(bytes, 0, deflate.deflate(bytes))
            return outputStream.toByteArray()
        }

        fun uncompress(byteArray: ByteArray): ByteArray {
            val inflater = Inflater()
            inflater.reset()
            inflater.setInput(byteArray)
            val outputStream = ByteArrayOutputStream()
            val bytes = ByteArray(bytesLength)
            while (!inflater.finished()) outputStream.write(bytes, 0, inflater.inflate(bytes))
            return outputStream.toByteArray()
        }
    }

    private fun IBackupManager.Node.toName() = "$date$id.$name"

    private fun IBackupManager.Node.match(string: String): Boolean = string.matches("^\\d{12}$id.$name$".toRegex())

    private fun String.containKey(key: String) = endsWith(".$key")

    private fun String.toNode() = if (this.length > 23) IBackupManager.Node(this.substring(24), substring(0, 12), substring(12, 23)) else null

    private fun getDateString() = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date(System.currentTimeMillis()))

}