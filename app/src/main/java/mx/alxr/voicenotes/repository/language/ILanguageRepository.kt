package mx.alxr.voicenotes.repository.language

import io.reactivex.Single

interface ILanguageRepository {

    fun loadAvailableLanguages(): Single<Unit>

}