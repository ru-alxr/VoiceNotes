package mx.alxr.voicenotes.feature.preload

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.feature.FEATURE_BACK
import mx.alxr.voicenotes.feature.FEATURE_SELECT_NATIVE_LANGUAGE
import mx.alxr.voicenotes.feature.FEATURE_WORKING
import mx.alxr.voicenotes.feature.IFeatureNavigation
import mx.alxr.voicenotes.repository.language.ILanguageRepository
import mx.alxr.voicenotes.utils.errors.ErrorSolution
import mx.alxr.voicenotes.utils.errors.IErrorMessageResolver
import mx.alxr.voicenotes.utils.errors.Interaction
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.rx.SingleDisposable
import java.util.concurrent.TimeUnit

class PreloadViewModel(
    private val languageRepository: ILanguageRepository,
    private val errorResolver: IErrorMessageResolver,
    private val navigation: IFeatureNavigation,
    private val logger: ILogger
) : ViewModel() {

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()

    private var mDisposable: Disposable? = null

    init {
        mLiveModel.value = Model()
        loadLanguages()
    }

    override fun onCleared() {
        mDisposable?.dispose()
        super.onCleared()
    }

    fun getModel(): LiveData<Model> {
        return mLiveModel
    }

    fun onErrorSolutionApplied() {
        val model = mLiveModel.value!!
        mLiveModel.value = model.copy(solution = ErrorSolution())
    }

    fun onRetrySelected() {
        mDisposable?.dispose()
        loadLanguages()
    }

    fun onSkipSelected() {
        val model = mLiveModel.value!!
        if (model.selectionFlag) navigation.navigateFeature(FEATURE_BACK)
        else navigation.navigateFeature(FEATURE_WORKING)
    }

    private fun loadLanguages() {
        mLiveModel.value = mLiveModel.value!!.copy(isInProgress = true)
        mDisposable = languageRepository
            .loadAvailableLanguages()
            .subscribeOn(Schedulers.io())
            .delay(300, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(SingleDisposable(this::onPreloadSuccess, this::onPreloadError))
    }

    private fun onPreloadError(throwable: Throwable) {
        logger.with(this).add("onPreloadError $throwable").log()
        mLiveModel.value = mLiveModel.value!!
            .copy(isInProgress = false, solution = errorResolver.resolve(throwable, Interaction.Alert))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPreloadSuccess(t: Unit) {
        mLiveModel.value?.apply {
            if (selectionFlag) {
                navigation.navigateFeature(FEATURE_SELECT_NATIVE_LANGUAGE, true)
            } else {
                navigation.navigateFeature(FEATURE_SELECT_NATIVE_LANGUAGE)
            }
        }
    }

    fun setSelectionFlag() {
        mLiveModel.value?.apply { mLiveModel.value = copy(selectionFlag = true) }
    }

}

data class Model(
    val isInProgress: Boolean = false,
    val selectionFlag: Boolean = false,
    val solution: ErrorSolution = ErrorSolution()
)