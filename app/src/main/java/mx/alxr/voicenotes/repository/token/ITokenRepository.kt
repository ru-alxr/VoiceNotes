package mx.alxr.voicenotes.repository.token

import io.reactivex.Single

interface ITokenRepository {

    fun getToken(): Single<String>

}