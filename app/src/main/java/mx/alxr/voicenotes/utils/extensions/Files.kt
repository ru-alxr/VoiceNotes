package mx.alxr.voicenotes.utils.extensions

import mx.alxr.voicenotes.utils.extensions.FileUtils.generateMD5
import java.io.File
import java.io.FileInputStream
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream

fun File.crc32(): Long {
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