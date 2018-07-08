package org.kreal.webdav.sync.backup

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class LocalStorage(file: File) : IStoraage {

    private val file: File

    private val state: Boolean

    init {
        if (file.exists() && file.isDirectory) {
            this.file = file
            state = true
        } else {
            this.file = File("")
            state = false
        }
    }

    override fun upload(name: String, inputStream: InputStream): Boolean {
        return if (state) {
            val outputStream = FileOutputStream(File(file, name))
            inputStream.copyTo(outputStream)
            outputStream.close()
            true
        } else false
    }

    override fun download(name: String): InputStream? {
        return if (state) {
            try {
                FileInputStream(File(file, name))
            } catch (e: Exception) {
                null
            }
        } else null
    }

    override fun list(): Array<String> = if (state) file.list() else emptyArray()

    override fun rename(name: String, newName: String): Boolean = state && try {
        File(file, name).renameTo(File(file, newName))
    } catch (e: Exception) {
        false
    }

    override fun delete(name: String): Boolean {
        return state && try {
            File(file, name).delete()
        } catch (e: Exception) {
            false
        }
    }

}