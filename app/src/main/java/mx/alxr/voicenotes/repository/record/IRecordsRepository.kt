package mx.alxr.voicenotes.repository.record

import io.reactivex.Flowable
import io.reactivex.Single
import mx.alxr.voicenotes.repository.language.LanguageEntity

const val DIRECTORY_NAME = "VoiceNotesMedia"

interface IRecordsRepository {

    fun insert(record: IRecord): Single<Unit>

    fun setLanguage(crc32: Long, entity: LanguageEntity): Single<Unit>

    fun getAll(isSynchronized:Boolean): Flowable<List<RecordEntity>>

    fun insert(record: RecordEntity)

}