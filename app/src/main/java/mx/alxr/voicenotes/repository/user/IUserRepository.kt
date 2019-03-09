package mx.alxr.voicenotes.repository.user

import io.reactivex.Flowable
import io.reactivex.Single
import mx.alxr.voicenotes.repository.language.LanguageEntity

interface IUserRepository {

    fun getUser(): Flowable<IUser>

    fun getUserSingle():Single<IUser>

    fun setUserNativeLanguage(language: LanguageEntity): Single<Unit>

    fun setNativeLanguageExplicitlyAsked(): Single<Unit>

}