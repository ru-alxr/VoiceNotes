package mx.alxr.voicenotes.feature.working.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.repository.user.IUserRepository

class HomeViewModel(private val userRepository: IUserRepository) : ViewModel() {

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

    fun onTouchOverMoved(pointerIsOut: Boolean) {
        mLiveModel.value?.apply {
            if (isPointerOut == pointerIsOut) return
            mLiveModel.value = copy(isPointerOut = pointerIsOut)
        }
    }

    fun onRecordingStopped(){
        mLiveModel.value?.apply {
            mLiveModel.value = copy(isRecordingInProgress = false)
        }
    }

    fun onRecordingStarted(){
        mLiveModel.value?.apply {
            mLiveModel.value = copy(isRecordingInProgress = true)
        }
    }

    fun onCancelRecordingHandled(){
        mLiveModel.value?.apply {
            mLiveModel.value = copy(isPointerOut = false)
        }
    }

}

data class Model(
    val language: String = "",
    val isSynchronizationEnabled: Boolean = false,
    val isPointerOut:Boolean = false,
    val isRecordingInProgress:Boolean = false
)