package mx.alxr.voicenotes.repository.user

import android.os.Environment
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.repository.language.LanguageDAO
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.repository.language.MAX_LANGUAGES
import mx.alxr.voicenotes.repository.record.DIRECTORY_NAME
import mx.alxr.voicenotes.repository.record.RecordDAO
import mx.alxr.voicenotes.repository.remote.firebaseuser.ProjectUser
import mx.alxr.voicenotes.utils.logger.ILogger
import java.io.File

class UserRepository(
    db: AppDatabase,
    private val logger: ILogger
) : IUserRepository {

    private val mUserDAO: UserDAO = db.userDataDAO()
    private val mLanguageDAO: LanguageDAO = db.languageDataDAO()
    private val mRecordsDAO: RecordDAO by lazy { db.recordDataDAO() }

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

                if (!user.languageCode.isEmpty()) {
                    val language = mLanguageDAO.getLanguage(user.languageCode)
                    language?.apply {
                        mLanguageDAO.insert(copy(position = position - MAX_LANGUAGES))
                    }
                }
                Single.just(updated)
            }
    }

    override fun setRestoreRecordsPerformed(): Single<Unit> {
        return getUserSingle()
            .flatMap {
                mUserDAO.insert(it.copy(isFetchingRecordsPerformed = true))
                Single.just(Unit)
            }
    }

    override fun signOut(): Single<Unit> {
        return Single.fromCallable {
            mUserDAO.insert(UserEntity())
            mRecordsDAO.deleteAll()
            try {
                val directory = File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME)
                logger
                    .with(this)
                    .add("signOut directory $DIRECTORY_NAME ${if (directory.exists()) "EXISTS" else "NOT EXISTS"}")
                    .log()
                if (directory.exists()) {
                    val result = clearFolder(directory, 0)
                    logger
                        .with(this@UserRepository)
                        .add("signOut directory $DIRECTORY_NAME ${if (!directory.exists()) "WAS REMOVED" else "WAS NOT REMOVED"} $result FILES WAS DELETED")
                        .log()
                }
            } catch (e: Exception) {
                logger.with(this).add("signOut error $e").log()
            }
            Unit
        }
    }

    private fun clearFolder(file : File, count:Int):Int{
        var result = 0
        if (file.isDirectory){
            val files = file.listFiles()
            for (every in files){
                result += clearFolder(every, 0)
            }
            file.delete()
        }
        else {
            if (file.delete()) result = 1
        }
        return result + count
    }

}