package mx.alxr.voicenotes.repository.user

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.repository.language.LanguageEntity
import java.lang.RuntimeException

class UserRepository(db: AppDatabase) : IUserRepository {

    private val mUserDAO: UserDAO = db.userDataDAO()

    override fun getUser(): Flowable<IUser> {
        return Flowable
            .fromCallable {
                val user = mUserDAO.getUserImmediately()
                if (user == null) mUserDAO
                    .insert(UserEntity(id = 1L, isAsked = false, languageCode = "", languageName = ""))
                Unit
            }
            .subscribeOn(Schedulers.io())
            .flatMap {
                mUserDAO.getUser()
            }
            .map {
                it[0]
            }
    }

    override fun setUserNativeLanguage(language: LanguageEntity): Single<Unit> {
        return Single
            .fromCallable {
                val user = mUserDAO.getUserImmediately()
                user?.apply {
                    mUserDAO.insert(copy(languageCode = language.code, languageName = language.name, isAsked = true))
                } ?: throw RuntimeException("DB has no user object")
                Unit
            }
            .subscribeOn(Schedulers.io())
    }

    override fun setNativeLanguageExplicitlyAsked(): Single<Unit> {
        return Single
            .fromCallable {
                val user = mUserDAO.getUserImmediately()
                user?.apply {
                    mUserDAO.insert(copy(isAsked = true))
                } ?: throw RuntimeException("DB has no user object")
                Unit
            }
            .subscribeOn(Schedulers.io())
    }

}