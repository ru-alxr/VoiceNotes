package mx.alxr.voicenotes.repository.user

import io.reactivex.Flowable
import io.reactivex.Single
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.repository.remote.firebaseuser.ProjectUser

interface IUserRepository {

    fun getUser(): Flowable<UserEntity>

    fun getUserSingle():Single<UserEntity>

    fun update(user: ProjectUser):Single<UserEntity>

    fun setUserNativeLanguage(language: LanguageEntity): Single<Unit>

    fun setNativeLanguageExplicitlyAsked(): Single<Unit>
    fun setRestoreRecordsPerformed(): Single<Unit>

}