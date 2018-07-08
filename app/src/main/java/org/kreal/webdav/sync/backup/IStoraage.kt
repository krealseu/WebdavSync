package org.kreal.webdav.sync.backup

import java.io.InputStream

interface IStoraage {

    fun upload(name: String, inputStream: InputStream): Boolean

    fun download(name: String): InputStream?

    fun list(): Array<String>

    fun rename(name: String, newName: String): Boolean

    fun delete(name: String): Boolean
}