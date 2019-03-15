package mx.alxr.voicenotes.repository.remote.firebaseuser

import io.reactivex.Single
import mx.alxr.voicenotes.feature.auth.ExtractedFirebaseUser

interface IRemoteUserRepository {

    fun getUser(extractedUser: ExtractedFirebaseUser): Single<ProjectUser>

    fun change(set: UserChangeSet): Single<ProjectUser>

}