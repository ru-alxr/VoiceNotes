package mx.alxr.voicenotes.feature.working.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.feature.recorder.IRecorder
import mx.alxr.voicenotes.feature.synchronizer.ISynchronizer
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.rx.SingleDisposable

class HomeViewModel(
    userRepository: IUserRepository,
    private val logger: ILogger,
    private val recorder: IRecorder,
    private val synchronizer: ISynchronizer
) : ViewModel() {

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()

    private val mDisposable: Disposable
    private var mStoreMediaDisposables: Disposable? = null

    init {
        mLiveModel.value = Model()
        mDisposable = userRepository
            .getUser()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                it?.apply {
                    mLiveModel.value = mLiveModel.value?.copy(languageCode = languageCode)
                }
            }
            .subscribe()
    }

    override fun onCleared() {
        mDisposable.dispose()
        mStoreMediaDisposables?.dispose()
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

    fun onRecordingStopped() {
        val store = mLiveModel.value?.isPointerOut != true
        logger.with(this).add("onRecordingStopped $store").log()
        recorder.stopRecording()
        mStoreMediaDisposables?.dispose()

        if (store) {
            val model = mLiveModel.value ?:return
            mStoreMediaDisposables = synchronizer
                .storeFile(recorder.getRecord(), model.languageCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(SingleDisposable<Unit>(
                    success = {
                        mLiveModel.value?.apply {
                            mLiveModel.value = copy(isRecordingInProgress = false, isStopRecordingRequested = false)
                        }
                    },
                    error = {
                        it.printStackTrace()
                    }
                ))
        } else {
            mLiveModel.value?.apply {
                mLiveModel.value = copy(isRecordingInProgress = false, isStopRecordingRequested = false)
            }
        }
    }

    fun onRecordingStarted() {
        mLiveModel.value?.apply {
            mLiveModel.value = copy(isRecordingInProgress = true)
        }
    }

    fun onRecordingUIReady() {
        logger.with(this).add("onRecordingStarted").log()
        recorder.startRecording()
    }

    fun onCancelRecordingHandled() {
        mLiveModel.value?.apply {
            mLiveModel.value = copy(isPointerOut = false)
        }
    }

    fun onStopRecordingRequested() {
        mLiveModel.value?.apply {
            if (!isRecordingInProgress) return@apply
            mLiveModel.value = copy(isStopRecordingRequested = true)
        }
    }

}

data class Model(
    val languageCode:String = "",
    val isPointerOut: Boolean = false,
    val isRecordingInProgress: Boolean = false,
    val isStopRecordingRequested: Boolean = false
)