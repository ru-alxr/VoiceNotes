package mx.alxr.voicenotes.feature.synchronizer

import io.reactivex.Single
import mx.alxr.voicenotes.repository.user.UserEntity

interface IRecordsFetcher {

    fun fetch(entity: UserEntity): Single<Int>

}