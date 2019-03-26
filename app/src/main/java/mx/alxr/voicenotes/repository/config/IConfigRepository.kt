package mx.alxr.voicenotes.repository.config

import io.reactivex.Single

interface IConfigRepository {

    fun getLanguages(): Single<String>

    /**
     * duration of audio file which fits of synchronous recognition (i.e. 60 sec)
     */
    fun getSynchronousDurationMillis():Single<Long>

    fun getServiceCredentials():Single<String>

}