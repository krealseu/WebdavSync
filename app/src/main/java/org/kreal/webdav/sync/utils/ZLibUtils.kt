package org.kreal.webdav.sync.utils

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

object ZLibUtils {
    private const val bytesLength = 1024
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