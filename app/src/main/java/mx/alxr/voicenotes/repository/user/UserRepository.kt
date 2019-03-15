package mx.alxr.voicenotes.repository.user

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.repository.language.LanguageDAO
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.repository.language.MAX_LANGUAGES
import mx.alxr.voicenotes.repository.remote.firebaseuser.ProjectUser
import mx.alxr.voicenotes.utils.logger.ILogger

class UserRepository(db: AppDatabase, private val logger:ILogger) : IUserRepository {

    private val mUserDAO: UserDAO = db.userDataDAO()
    private val mLanguageDAO: LanguageDAO = db.languageDataDAO()

    private fun dbchck(): Single<Unit> {
        return Single
            .fromCallable {
                if (mUserDAO.getUserImmediately() == null) mUserDAO.insert(UserEntity())
                Unit
            }
            .subscribeOn(Schedulers.io())
    }

    private fun dbchckF(): Flowable<Unit> {
        return Flowable
            .fromCallable {
                if (mUserDAO.getUserImmediately() == null) mUserDAO.insert(UserEntity())
            }
            .subscribeOn(Schedulers.io())
    }

    override fun getUserSingle(): Single<UserEntity> {
        return dbchck().flatMap { mUserDAO.getUserSingle() }
    }

    override fun getUser(): Flowable<UserEntity> {
        return dbchckF().flatMap { mUserDAO.getUser() }
    }

    override fun setUserNativeLanguage(language: LanguageEntity): Single<Unit> {
        return getUserSingle()
            .flatMap {
                it.apply {
                    val changed = languageCode != language.code || languageName != language.name
                    if (changed) {
                        mUserDAO.insert(
                            copy(
                                languageCode = language.code,
                                languageName = language.name,
                                isLanguageRequested = true
                            )
                        )
                        mLanguageDAO.insert(language.copy(position = language.position - MAX_LANGUAGES))
                    }
                }
                Single.just(Unit)
            }
    }

    override fun setNativeLanguageExplicitlyAsked(): Single<Unit> {
        return getUserSingle()
            .flatMap {
                it.apply {
                    if (!isLanguageRequested) mUserDAO.insert(copy(isLanguageRequested = true))
                }
                Single.just(Unit)
            }
    }

    override fun update(user: ProjectUser): Single<UserEntity> {
        return getUserSingle()
            .flatMap {
                val updated = it.copy(
                    firebaseUserProvider = user.authProvider,
                    firebaseUserId = user.uid,

                    email = user.email,
                    displayName = user.displayName,

                    languageCode = user.languageCode,
                    languageName = user.languageName,
                    languageNameEnglish = user.languageNameEnglish
                )
                logger.with(this@UserRepository).add("Insert $updated").log()
                mUserDAO.insert(updated)
                Single.just(updated)
            }
    }
}