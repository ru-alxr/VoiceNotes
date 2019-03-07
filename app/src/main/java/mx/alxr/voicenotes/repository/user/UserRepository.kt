package mx.alxr.voicenotes.repository.user

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.db.AppDatabase
import java.lang.RuntimeException

class UserRepository(db: AppDatabase) : IUserRepository {

    private val mUserDAO: UserDAO = db.userDataDAO()

    override fun getUser(): Flowable<IUser> {
        return Flowable
            .fromCallable {
                val user = mUserDAO.getUserImmediately()
                if (user == null) mUserDAO.insert(UserEntity(id = 1L, language = "", isAsked = false))
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

    override fun setUserNativeLanguage(code: String): Single<Unit> {
        return Single
            .fromCallable {
                val user = mUserDAO.getUserImmediately()
                user?.apply {
                    mUserDAO.insert(copy(language = code, isAsked = true))
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