package mx.alxr.voicenotes.feature.working.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.feature.FEATURE_PRELOAD
import mx.alxr.voicenotes.feature.FEATURE_SIGN_OUT
import mx.alxr.voicenotes.feature.IFeatureNavigation
import mx.alxr.voicenotes.repository.user.IUserRepository

class SettingsViewModel(
    private val userRepository: IUserRepository,
    private val nav: IFeatureNavigation
) : ViewModel() {

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()

    private val mDisposable: Disposable
    private var mSignOutDisposable: Disposable? = null

    init {
        mLiveModel.value = Model()
        mDisposable = userRepository
            .getUser()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                it?.apply {
                    mLiveModel.value = mLiveModel.value?.copy(
                        language = languageName,
                        authProvider = firebaseUserProvider,
                        email = email,
                        displayName = displayName
                    )
                }
            }
            .subscribe()
    }

    override fun onCleared() {
        mDisposable.dispose()
        super.onCleared()
    }

    fun getLiveModel(): LiveData<Model> {
        mLiveModel.value?.apply { mLiveModel.value = this }
        return mLiveModel
    }

    fun onLanguageChangeSelected() {
        nav.navigateFeature(FEATURE_PRELOAD, true)
    }

    fun onSignOutRequested() {
        val model = mLiveModel.value!!
        mLiveModel.value = model.copy(signOut = true)
    }

    fun onSignedOut() {
        val model = mLiveModel.value!!
        mLiveModel.value = model.copy(signOut = false)
        mSignOutDisposable?.dispose()
        mSignOutDisposable = userRepository
            .signOut()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { nav.navigateFeature(FEATURE_SIGN_OUT) }
            .subscribe()
    }
}

data class Model(
    val language: String = "",
    val signOut: Boolean = false,
    val authProvider:String = "",
    val displayName:String = "",
    val email:String = ""
)