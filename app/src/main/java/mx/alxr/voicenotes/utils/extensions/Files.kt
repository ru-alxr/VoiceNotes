package mx.alxr.voicenotes.utils.extensions

import android.util.Base64
import mx.alxr.voicenotes.utils.extensions.FileUtils.generateMD5
import java.io.*
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream

fun File.crc32(): Long {
    if (!exists()) return -2
    CheckedInputStream(FileInputStream(absolutePath), CRC32()).use {
        val buf = ByteArray(android.os.StatFs(parentFile.path).blockSizeLong.toInt())
        var i: Int
        do {i = it.read(buf)} while (i >= 0)
        return it.checksum.value
    }
}

fun File.md5Hash():String{
    return generateMD5(this)
}

private fun File.getFileString64(): String {
    var ous: ByteArrayOutputStream? = null
    var ios: InputStream? = null
    try {
        val buffer = ByteArray(4096)
        ous = ByteArrayOutputStream()
        ios = FileInputStream(this)
        var read = 0

        do {
            read = ios.read(buffer)
            if (read == -1) break
            ous.write(buffer, 0, read)
        } while (true)
    } finally {
        try {
            ous?.close()
        } catch (e: IOException) {
            // swallow, since not that important
        }
        try {
            ios?.close()
        } catch (e: IOException) {
            // swallow, since not that important
        }
    }
    return Base64.encodeToString(ous!!.toByteArray(), Base64.NO_WRAP)
}