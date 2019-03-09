package mx.alxr.voicenotes.feature.working

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mx.alxr.voicenotes.utils.logger.ILogger

const val TAB_HOME = 0
const val TAB_SETTINGS = 1

class WorkingViewModel(val logger: ILogger) : ViewModel() {

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()

    private var model: Model = Model()

    fun getLiveModel(): LiveData<Model> {
        mLiveModel.value = model
        return mLiveModel
    }

    fun onTabSelected(tab: Int) {
        logger.with(this).add("onTabSelected $tab").log()
        model = model.copy(selectedTab = tab)
        mLiveModel.value = model
    }

}

data class Model(val selectedTab: Int = TAB_HOME)