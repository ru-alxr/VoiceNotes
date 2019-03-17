package mx.alxr.voicenotes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.feature.*
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.repository.user.UserEntity
import mx.alxr.voicenotes.utils.rx.SingleDisposable

class MainViewModel(
    private val featureNavigation: IFeatureNavigation,
    userRepository: IUserRepository
) : ViewModel(), IHandler {

    private var mDisposable: Disposable? = null

    init {
        featureNavigation.attach(this)
        mDisposable = userRepository
            .getUserSingle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(SingleDisposable<UserEntity>(
                success = {
                    val model = mLiveUserState.value ?: UserState()
                    if (it.firebaseUserId.isEmpty()) {
                        mLiveUserState.value = model.copy(feature = FEATURE_AUTH, args = null)
                        return@SingleDisposable
                    }
                    if (!it.isFetchingRecordsPerformed && !it.languageCode.isEmpty()){
                        mLiveUserState.value = model.copy(feature = FEATURE_LOAD_RECORDS, args = null)
                        return@SingleDisposable
                    }
                    if (it.isLanguageRequested) {
                        mLiveUserState.value = model.copy(feature = FEATURE_WORKING, args = null)
                        return@SingleDisposable
                    }
                    if (!it.languageCode.isEmpty()) {
                        mLiveUserState.value = model.copy(feature = FEATURE_WORKING, args = null)
                        return@SingleDisposable
                    }
                    mLiveUserState.value = model.copy(feature = FEATURE_PRELOAD, args = null)
                },
                error = {
                    it.printStackTrace()
                }
            )
            )
    }

    override fun onCleared() {
        mDisposable?.dispose()
        featureNavigation.detach()
        super.onCleared()
    }

    private val mLiveUserState: MutableLiveData<UserState> = MutableLiveData()

    fun getFeature(): LiveData<UserState> {
        return mLiveUserState
    }

    override fun onFeatureRequested(target: Int) {
        mLiveUserState.value = mLiveUserState.value?.copy(feature = target, args = null) ?: UserState(target)
    }

    override fun onFeatureRequested(target: Int, args: Any) {
        mLiveUserState.value = mLiveUserState.value?.copy(feature = target, args = args) ?: UserState(target, args)
    }

}

data class UserState(val feature: Int = FEATURE_INIT, val args: Any? = null)