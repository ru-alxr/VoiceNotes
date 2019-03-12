package mx.alxr.voicenotes.repository.media

import io.reactivex.Single
import java.io.File

const val FILE_NAME_PATTERN = "Voice-%s.%s"
const val DATE_PATTERN = "yyyy-MM-dd'T'HH-mm-ss"

interface IMediaStorage {

    fun storeFile(file: File): Single<Unit>

    fun getFile(name:String, crc32:Long):Single<File>

}