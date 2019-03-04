package mx.alxr.voicenotes.feature.init

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InitViewModel : ViewModel() {

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()

    init {
        mLiveModel.value = Model()
    }

    fun getLiveModel(): LiveData<Model> {
        return mLiveModel
    }

}

data class Model(val popupMessage:String = "",
                 val isLoading:Boolean = false)