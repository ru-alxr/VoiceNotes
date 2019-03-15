package mx.alxr.voicenotes.repository.remote.firebaseuser

import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Single
import io.reactivex.SingleEmitter
import mx.alxr.voicenotes.feature.auth.ExtractedFirebaseUser
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.repository.user.UserEntity
import mx.alxr.voicenotes.utils.logger.ILogger

class RemoteUserRepository(
    private val store: FirebaseFirestore,
    @Suppress("unused") private val logger: ILogger
) : IRemoteUserRepository {

    private class UserWrapper(val uid: String, val extractedUser: ExtractedFirebaseUser? = null)

    override fun getUser(extractedUser: ExtractedFirebaseUser): Single<ProjectUser> {
        return Single.create { emitter -> getUserTask(UserWrapper(extractedUser.uid, extractedUser), emitter) }
    }

    override fun change(set: UserChangeSet): Single<ProjectUser> {
        return Single.create { emitter -> updateUserTask(set, emitter) }
    }

    private fun getUserTask(wrapper: UserWrapper, emitter: SingleEmitter<ProjectUser>) {
        val userReference = store
            .collection("users")
            .document(wrapper.uid)
        userReference
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.apply {
                    if (emitter.isDisposed) return@addOnSuccessListener
                    try {
                        if (!snapshot.exists()) {
                            if (wrapper.extractedUser != null) {
                                createRecord(wrapper.extractedUser, emitter)
                            } else {
                                onFailure(emitter, NullPointerException("No user"))
                            }
                            return@addOnSuccessListener
                        }
                        snapshot.data!!.apply {
                            val user = ProjectUser(
                                uid = get("uid")!!.toString(),
                                languageCode = get("language_code")!!.toString(),
                                languageName = get("language_name")!!.toString(),
                                languageNameEnglish = get("language_name_english")!!.toString(),
                                displayName = get("display_name")!!.toString(),
                                email = get("email")!!.toString(),
                                authProvider = get("auth_provider")!!.toString()
                            )
                            emitter.onSuccess(user)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onFailure(emitter, NullPointerException("No user"))
                    }
                }
            }
            .addOnFailureListener { exception -> onFailure(emitter, exception) }
    }

    private fun <T> onFailure(emitter: SingleEmitter<T>, e: Throwable) {
        if (emitter.isDisposed) return
        emitter.onError(e)
    }

    private fun createRecord(extractedUser: ExtractedFirebaseUser, emitter: SingleEmitter<ProjectUser>) {
        val user = HashMap<String, Any>()
        user["uid"] = extractedUser.uid
        user["auth_provider"] = extractedUser.providerId
        user["email"] = extractedUser.email ?: ""
        user["display_name"] = extractedUser.displayName ?: ""
        user["language_code"] = ""
        user["language_name"] = ""
        user["language_name_english"] = ""
        store
            .collection("users")
            .document(extractedUser.uid)
            .set(user)
            .addOnSuccessListener {
                getUserTask(UserWrapper(extractedUser.uid, extractedUser), emitter)
            }
            .addOnFailureListener {
                it.printStackTrace()
                onFailure(emitter, it)
            }
    }

    private fun updateUserTask(set: UserChangeSet, emitter: SingleEmitter<ProjectUser>) {
        val userReference = store
            .collection("users")
            .document(set.uid)
        store
            .runTransaction { transaction ->
                transaction
                    .update(
                        userReference,
                        set.change
                    )
                null
            }
            .addOnSuccessListener { getUserTask(UserWrapper(set.uid), emitter) }
            .addOnFailureListener { onFailure(emitter, it) }
    }

}