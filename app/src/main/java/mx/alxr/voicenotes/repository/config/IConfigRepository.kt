package mx.alxr.voicenotes.repository.config

import io.reactivex.Single

interface IConfigRepository {

    fun getLanguages(): Single<String>

}