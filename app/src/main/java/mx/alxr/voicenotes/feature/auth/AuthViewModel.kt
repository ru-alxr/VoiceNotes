package mx.alxr.voicenotes.feature.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.feature.FEATURE_LOAD_RECORDS
import mx.alxr.voicenotes.feature.FEATURE_PRELOAD
import mx.alxr.voicenotes.feature.IFeatureNavigation
import mx.alxr.voicenotes.repository.remote.firebaseuser.IRemoteUserRepository
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.repository.user.UserEntity
import mx.alxr.voicenotes.utils.errors.ErrorSolution
import mx.alxr.voicenotes.utils.errors.IErrorMessageResolver
import mx.alxr.voicenotes.utils.errors.Interaction
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.rx.SingleDisposable

class AuthViewModel(
    private val navigation: IFeatureNavigation,
    private val userRepository: IUserRepository,
    private val errorResolver: IErrorMessageResolver,
    private val remoteUserRepository: IRemoteUserRepository,
    @Suppress("unused") private val mLogger: ILogger
) : ViewModel() {

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()

    init {
        mLiveModel.value = Model()
    }

    fun onAuthSuccess(user: FirebaseUser, @Suppress("UNUSED_PARAMETER") isNewUser: Boolean) {
        val extract = user.extract(user.uid)
        if (extract == null) {
            onAuthFail()
            return
        }
        mDisposable?.dispose()
        mDisposable = remoteUserRepository
            .getUser(extract)
            .observeOn(Schedulers.io())
            .flatMap { userRepository.update(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(SingleDisposable<UserEntity>(
                success = {
                    if (isNewUser) {
                        navigation.navigateFeature(FEATURE_PRELOAD)
                    }else{
                        navigation.navigateFeature(FEATURE_LOAD_RECORDS)
                    }
                },
                error = {
                    val model = mLiveModel.value!!
                    mLiveModel.value = model.copy(
                        solution = errorResolver.resolve(it, Interaction.Snack),
                        signOut = true
                    )
                }
            ))
    }

    private fun FirebaseUser.extract(uid: String): ExtractedFirebaseUser? {
        val provider: String = providers?.extractProviderId() ?: return null
        return providerData.getUser(provider, uid)
    }

    private fun List<String>.extractProviderId(): String? {
        return if (isEmpty()) null else get(0)
    }

    private fun List<UserInfo>.getUser(provider: String, uid: String): ExtractedFirebaseUser? {
        for (info in this) {
            if (info.providerId == provider) return ExtractedFirebaseUser(
                email = info.email ?: "",
                uid = uid,
                displayName = info.displayName ?: "",
                providerId = provider
            )
        }
        return null
    }

    fun onAuthFail() {
        val model = mLiveModel.value!!
        mLiveModel.value = model.copy(errorMessage = R.string.auth_fail, signOut = true)
    }

    private var mDisposable: Disposable? = null


    override fun onCleared() {
        mDisposable?.dispose()
        super.onCleared()
    }

    fun getModel(): LiveData<Model> {
        return mLiveModel
    }

    fun onErrorMessageApplied() {
        val model = mLiveModel.value!!
        mLiveModel.value = model.copy(errorMessage = 0)
    }

    fun onErrorSolutionApplied() {
        val model = mLiveModel.value!!
        mLiveModel.value = model.copy(solution = ErrorSolution())
    }

    fun onSignedOut() {
        val model = mLiveModel.value!!
        mLiveModel.value = model.copy(signOut = false)
    }

}

data class Model(
    val errorMessage: Int = 0,
    val signOut: Boolean = true,
    val solution: ErrorSolution = ErrorSolution()
)