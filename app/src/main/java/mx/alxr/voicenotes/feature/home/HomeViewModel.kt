package mx.alxr.voicenotes.feature.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.repository.user.IUser
import mx.alxr.voicenotes.repository.user.IUserRepository

class HomeViewModel(private val userRepository: IUserRepository) : ViewModel() {

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()

    private val mDisposable:Disposable
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
        return mLiveModel
    }

}

data class Model(val language:String = "",
                 val isSynchronizationEnabled:Boolean = false)