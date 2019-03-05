package mx.alxr.voicenotes.repository.user

import io.reactivex.Flowable

interface IUserRepository {

    fun getUser(): Flowable<IUser>

}