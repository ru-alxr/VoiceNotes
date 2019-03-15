package mx.alxr.voicenotes.repository.record

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.utils.errors.ProjectException
import mx.alxr.voicenotes.utils.logger.ILogger

class RecordsRepository(db: AppDatabase,
                        private val logger: ILogger,
                        private val repo:IUserRepository) : IRecordsRepository {

    private val dao: RecordDAO = db.recordDataDAO()

    override fun insert(record: IRecord): Single<Unit> {
        return repo
            .getUserSingle()
            .flatMap {
                val entity = RecordEntity(
                    fileName = record.getName(),
                    crc32 = record.getCRC32(),
                    duration = record.getDuration(),
                    transcription = record.getTranscription(),
                    date = record.getDate(),
                    languageCode = record.getLanguageCode(),
                    userId = it.firebaseUserId
                )
                dao.insert(entity)
                logger.with(this).add("Insert $record").log()
                Single.just(Unit)
            }
            .subscribeOn(Schedulers.io())
    }

    override fun setLanguage(crc32: Long, entity: LanguageEntity): Single<Unit> {
        return dao
            .getRecordSingle(crc32.toString())
            .flatMap {
                if (it.isEmpty()) throw ProjectException(R.string.error_no_records)
                val record = it[0]
                val updated = record.copy(languageCode = entity.code, isSynchronized = false)
                dao.insert(updated)
                logger.with(this).add("Update $updated").log()
                Single.just(Unit)
            }
            .subscribeOn(Schedulers.io())
    }

}