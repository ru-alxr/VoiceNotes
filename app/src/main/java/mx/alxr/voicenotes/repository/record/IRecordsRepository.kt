package mx.alxr.voicenotes.repository.record

import io.reactivex.Flowable
import io.reactivex.Single
import mx.alxr.voicenotes.feature.synchronizer.AudioFile
import mx.alxr.voicenotes.repository.language.LanguageEntity

const val DIRECTORY_NAME = "VoiceNotesMedia"

interface IRecordsRepository {

    fun insert(record: AudioFile): Single<Unit>

    fun getCurrent(record: RecordEntity):Single<RecordEntity>

    fun setLanguage(uniqueId: String, entity: LanguageEntity): Single<Unit>

    fun getAll(isSynchronized:Boolean): Flowable<List<RecordEntity>>

    fun insert(record: RecordEntity)

    fun markDeleted(record: RecordEntity):Single<Unit>

    fun delete(uniqueId: String)

}