package mx.alxr.voicenotes.repository.user

import io.reactivex.Flowable
import io.reactivex.Single

interface IUserRepository {

    fun getUser(): Flowable<IUser>

    fun setUserNativeLanguage(code:String): Single<Unit>

    fun setNativeLanguageExplicitlyAsked(): Single<Unit>

}