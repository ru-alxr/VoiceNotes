package mx.alxr.voicenotes.repository.record

import io.reactivex.Single
import mx.alxr.voicenotes.repository.language.LanguageEntity

interface IRecordsRepository {

    fun insert(record: IRecord): Single<Unit>

    fun setLanguage(crc32: Long, entity: LanguageEntity): Single<Unit>

}