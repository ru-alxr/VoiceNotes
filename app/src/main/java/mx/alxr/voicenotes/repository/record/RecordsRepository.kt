package mx.alxr.voicenotes.repository.record

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.feature.synchronizer.AudioFile
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.utils.errors.ProjectException
import mx.alxr.voicenotes.utils.logger.ILogger

class RecordsRepository(
    db: AppDatabase,
    private val logger: ILogger,
    private val repo: IUserRepository
) : IRecordsRepository {

    private val dao: RecordDAO = db.recordDataDAO()

    override fun getAll(isSynchronized: Boolean): Flowable<List<RecordEntity>> {
        return dao.getAll(isSynchronized)
    }

    override fun insert(record: RecordEntity) {
        dao.insert(record)
    }

    override fun insert(record: AudioFile): Single<Unit> {
        return repo
            .getUserSingle()
            .flatMap {
                val entity = RecordEntity(
                    fileName = record.name,
                    crc32 = record.crc32,
                    duration = record.duration,
                    date = record.date,
                    languageCode = record.language,
                    userId = it.firebaseUserId,
                    isFileDownloaded = true,
                    uniqueId = record.uniqueId,
                    sampleRateHertz = record.sampleRateHertz,
                    encoding = record.encoding
                )
                dao.insert(entity)
                logger.with(this).add("Insert $record").log()
                Single.just(Unit)
            }
            .subscribeOn(Schedulers.io())
    }

    override fun setLanguage(uniqueId: String, entity: LanguageEntity): Single<Unit> {
        return dao
            .getRecordSingle(uniqueId)
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

    override fun markDeleted(record: RecordEntity): Single<Unit> {
        return Single
            .fromCallable {
                val updated = record.copy(isDeleted = true, isSynchronized = false)
                dao.insert(updated)
                logger.with(this).add("Update $updated").log()
                Unit
            }
            .subscribeOn(Schedulers.io())
    }

    override fun delete(uniqueId: String) {
        val record = dao.getRecord(uniqueId)
        record?.apply {
            dao.delete(this)
            logger.with(this@RecordsRepository).add("delete $uniqueId seems successful").log()
        } ?: logger.with(this@RecordsRepository).add("no local record $uniqueId, nothing to delete").log()
    }

    override fun getCurrent(record: RecordEntity): Single<RecordEntity> {
        return Single.fromCallable { dao.getRecord(record.uniqueId) }
    }
}