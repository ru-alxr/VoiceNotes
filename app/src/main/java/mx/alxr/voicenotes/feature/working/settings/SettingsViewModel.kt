package mx.alxr.voicenotes.feature.working.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.feature.FEATURE_PRELOAD
import mx.alxr.voicenotes.feature.IFeatureNavigation
import mx.alxr.voicenotes.repository.user.IUserRepository

class SettingsViewModel(userRepository: IUserRepository,
                        private val nav:IFeatureNavigation) : ViewModel() {

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()

    private val mDisposable: Disposable

    init {
        mLiveModel.value = Model()
        mDisposable = userRepository
            .getUser()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                it?.apply {
                    mLiveModel.value = mLiveModel.value?.copy(language = getNativeLanguage())
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

    fun onLanguageChangeSelected(){
        nav.navigateFeature(FEATURE_PRELOAD, true)
    }

}

data class Model(
    val language: String = "",
    val isSynchronizationEnabled: Boolean = false
)