package mx.alxr.voicenotes.repository.record

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.utils.logger.ILogger

class RecordsRepository(db: AppDatabase, val logger:ILogger) : IRecordsRepository {

    private val dao: RecordDAO = db.recordDataDAO()

    override fun insert(record: IRecord): Single<Unit> {
        return Single
            .fromCallable {
                val entity = RecordEntity(
                    fileName = record.getName(),
                    crc32 = record.getCRC32(),
                    duration = record.getDuration(),
                    transcription = record.getTranscription(),
                    date = record.getDate(),
                    languageCode = record.getLanguageCode()
                )
                dao.insert(entity)
                logger.with(this).add("Insert $record").log()
                Unit
            }
            .subscribeOn(Schedulers.io())
    }

}