package org.kreal.webdav.sync.backup

import java.io.InputStream

interface IBackupManager {

    fun backup(key: String, info: String): Boolean

    fun listKey(key: String): Array<Node>

    fun getKey(node: Node): String?

    fun backupFile(name: String, inputStream: InputStream): Boolean

    fun listFile(name: String): Array<Node>

    fun getFile(node: Node): InputStream?

    data class Node(val name: String, val date: String, val id: String)
}