package mx.alxr.voicenotes.repository.language

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.errors.ProjectException

class LanguageRepository : ILanguageRepository {

    companion object {
        const val DEBUG = true
    }

    override fun loadAvailableLanguages(): Single<Unit> {
        return Single
            .fromCallable {
                Unit
            }
            .subscribeOn(Schedulers.io())
            .flatMap {
                Thread.sleep(3000L)
                if (DEBUG) throw ProjectException(R.string.debug_crash_message)
                Single.just(Unit)
            }
    }

}