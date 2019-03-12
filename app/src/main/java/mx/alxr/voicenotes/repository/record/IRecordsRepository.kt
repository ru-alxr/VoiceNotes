package mx.alxr.voicenotes.repository.record

import io.reactivex.Single

interface IRecordsRepository {

    fun insert(record:IRecord): Single<Unit>

}