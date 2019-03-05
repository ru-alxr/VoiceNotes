package mx.alxr.voicenotes.repository.user

import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.db.AppDatabase

class UserRepository(db: AppDatabase) : IUserRepository {

    private val mUserDAO: UserDAO = db.userDataDAO()

    override fun getUser(): Flowable<IUser> {
        return Flowable
            .fromCallable {
                val user = mUserDAO.getUserImmediately()
                if (user == null) mUserDAO.insert(UserEntity())
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

}