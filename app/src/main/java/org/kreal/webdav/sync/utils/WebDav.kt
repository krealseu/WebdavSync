package org.kreal.webdav.sync.utils

import de.aflx.sardine.Sardine
import de.aflx.sardine.SardineFactory
import de.aflx.sardine.impl.SardineException
import java.io.IOException
import java.io.InputStream

/**
 * WebDav的单文件夹类容处理
 * 在坚果云上做的测试
 */
class WebDav(account: String, password: String, private val server: String) {
    private val sardine: Sardine = SardineFactory.begin(account, password)

    private val state: Boolean

    init {
        state = try {
            val res = sardine.list(server, 0)
            res.isNotEmpty() && res[0].isDirectory
        } catch (e: SardineException) {
            if (e.statusCode == 404) {
                try {
                    sardine.createDirectory(server)
                    true
                } catch (e: SardineException) {
                    throw e
                }
            } else {
                throw e
                false
            }
        }
    }

    fun upLoad(name: String, inputStream: InputStream) {
        if (state) {
            try {
                sardine.put("$server/$name", inputStream)
            } catch (e: IOException) {
                throw e
            }
        }
    }

    fun download(name: String): InputStream {
        if (state) {
            val url = getUri(name)
            try {
                return sardine[url]
            } catch (e: IOException) {
                throw e
            }
        } else throw Exception("WebDav uri is error")
    }

    fun list(): List<String> {
        val result: MutableList<String> = arrayListOf()
        if (state) {
            try {
                val resources = sardine.list(server)
                resources.forEachIndexed { index, davResource ->
                    if (index != 0)
                        result.add(davResource.name)
                }
            } catch (e: Exception) {
                result.clear()
            }
        }
        return result
    }

    fun rename(name: String, new: String): Boolean {
        return if (state) {
            try {
                sardine.move(getUri(name), getUri(new))
                true
            } catch (e: Exception) {
                false
            }
        } else false
    }

    private fun getUri(name: String) = "$server/$name"
}