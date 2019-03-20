package mx.alxr.voicenotes.feature.synchronizer

import io.reactivex.Single
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.utils.errors.ProjectException
import java.io.File

const val FILE_NAME_PATTERN = "Voice-%s.%s"
const val DATE_PATTERN = "yyyy-MM-dd'T'HH-mm-ss"

interface ISynchronizer {

    fun onStart()

    fun onStop()

    fun fetchFile(entity: RecordEntity): Single<File>

    fun storeFile(file: File, languageCode:String): Single<Unit>

    fun getFile(entity: RecordEntity):Single<File>

    @Throws(ProjectException::class)
    fun getDirectory():File

}