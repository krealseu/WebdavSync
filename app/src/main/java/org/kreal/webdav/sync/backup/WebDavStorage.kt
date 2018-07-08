package org.kreal.webdav.sync.backup

import de.aflx.sardine.Sardine
import de.aflx.sardine.SardineFactory
import de.aflx.sardine.impl.SardineException
import java.io.InputStream

/**
 * WebDav的单文件夹类容处理
 * 在坚果云上做的测试
 */
class WebDavStorage(account: String, password: String, private val server: String) : IStoraage {

    private val sardine: Sardine = SardineFactory.begin(account, password)

    private val state: Boolean by lazy {
        try {
            val res = sardine.list(server, 0)
            res.isNotEmpty() && res[0].isDirectory
        } catch (e: SardineException) {
            if (e.statusCode == 404) {
                try {
                    sardine.createDirectory(server)
                    true
                } catch (e: SardineException) {
                    false
                }
            } else {
                false
            }
        }
    }

    override fun upload(name: String, inputStream: InputStream): Boolean = state && try {
        sardine.put(convert2Url(name), inputStream)
        true
    } catch (e: Exception) {
        false
    }

    override fun download(name: String): InputStream? = if (state) {
        try {
            sardine[convert2Url(name)]
        } catch (e: Exception) {
            null
        }
    } else null

    override fun list(): Array<String> {
        if (state) {
            return try {
                val resources = sardine.list(server)
                if (resources.size > 1) {
                    val result: Array<String> = Array(resources.size - 1) { "" }
                    resources.forEachIndexed { index, davResource ->
                        if (index != 0) {
                            result[index - 1] = davResource.name
                        }
                    }
                    result
                } else emptyArray()
            } catch (e: Exception) {
                emptyArray()
            }
        } else return emptyArray()
    }

    override fun rename(name: String, newName: String): Boolean = state && try {
        sardine.move(convert2Url(name), convert2Url(newName))
        true
    } catch (e: Exception) {
        false
    }

    override fun delete(name: String): Boolean = state && try {
        sardine.delete(convert2Url(name))
        true
    } catch (e: Exception) {
        false
    }

    //坚果云的测试
    @Throws
    private fun convert2Url(name: String): String {
        if (name.contains("[:/*?<>|\\\\]".toRegex()))
            throw Exception("Illegal name")
        val n = name.replace(" ", "%20")
        return "$server/$n"
    }
}