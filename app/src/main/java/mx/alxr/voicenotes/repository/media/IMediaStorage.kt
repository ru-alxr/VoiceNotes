package mx.alxr.voicenotes.repository.media

import io.reactivex.Single
import mx.alxr.voicenotes.utils.errors.ProjectException
import java.io.File

const val FILE_NAME_PATTERN = "Voice-%s.%s"
const val DATE_PATTERN = "yyyy-MM-dd'T'HH-mm-ss"

interface IMediaStorage {

    fun storeFile(file: File, languageCode:String): Single<Unit>

    fun getFile(name:String, crc32:Long):Single<File>


    @Throws(ProjectException::class)
    fun getDirectory():File

}