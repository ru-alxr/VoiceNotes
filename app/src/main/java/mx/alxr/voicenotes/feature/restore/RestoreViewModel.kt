package mx.alxr.voicenotes.feature.restore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.feature.FEATURE_BACK
import mx.alxr.voicenotes.feature.FEATURE_PRELOAD
import mx.alxr.voicenotes.feature.FEATURE_WORKING
import mx.alxr.voicenotes.feature.IFeatureNavigation
import mx.alxr.voicenotes.feature.synchronizer.IRecordsFetcher
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.utils.errors.ErrorSolution
import mx.alxr.voicenotes.utils.errors.IErrorMessageResolver
import mx.alxr.voicenotes.utils.errors.Interaction
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.rx.SingleDisposable

class RestoreViewModel(
    private val logger: ILogger,
    private val navigation: IFeatureNavigation,
    private val errorResolver: IErrorMessageResolver,
    private val userRepository: IUserRepository,
    private val fetcher: IRecordsFetcher
) : ViewModel() {

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()

    private var mDisposable: Disposable? = null

    init {
        mLiveModel.value = Model()
        fetchData()
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
        fetchData()
    }

    fun onSkipSelected() {
        mDisposable?.dispose()
        mDisposable = userRepository
            .setRestoreRecordsPerformed()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(SingleDisposable(::onComplete))
    }

    private fun onComplete(@Suppress("UNUSED_PARAMETER") u: Unit) {
        val model = mLiveModel.value!!
        if (model.selectionFlag) navigation.navigateFeature(FEATURE_BACK)
        else {
            if (model.userLanguageCode.isEmpty()) {
                navigation.navigateFeature(FEATURE_PRELOAD)
            } else {
                navigation.navigateFeature(FEATURE_WORKING)
            }
        }
    }

    fun onResultShown() {
        onSkipSelected()
    }

    private fun fetchData() {
        mLiveModel.value = mLiveModel.value!!.copy(isInProgress = true)
        mDisposable?.dispose()
        mDisposable = userRepository
            .getUserSingle()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                mLiveModel.value = mLiveModel.value!!
                    .copy(userLanguageCode = it.languageCode)
            }
            .flatMap { fetcher.fetch(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(
                SingleDisposable(
                    ::onRestoreComplete,
                    ::onRestoreError
                )
            )
    }

    private fun onRestoreComplete(count: Int) {
        mLiveModel.value = mLiveModel.value!!.copy(restoreResult = count, isInProgress = false)
    }

    private fun onRestoreError(throwable: Throwable) {
        logger.with(this).add("onPreloadError $throwable").log()
        mLiveModel.value = mLiveModel.value!!
            .copy(isInProgress = false, solution = errorResolver.resolve(throwable, Interaction.Alert))
    }

    fun setSelectionFlag() {
        mLiveModel.value?.apply { mLiveModel.value = copy(selectionFlag = true) }
    }

}

data class Model(
    val isInProgress: Boolean = false,
    val selectionFlag: Boolean = false,
    val userLanguageCode: String = "",
    val solution: ErrorSolution = ErrorSolution(),
    val restoreResult: Int = -1
)