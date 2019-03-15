package mx.alxr.voicenotes.feature.selector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.feature.FEATURE_BACK
import mx.alxr.voicenotes.feature.FEATURE_WORKING
import mx.alxr.voicenotes.feature.IFeatureNavigation
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.repository.record.IRecordsRepository
import mx.alxr.voicenotes.repository.remote.firebaseuser.IRemoteUserRepository
import mx.alxr.voicenotes.repository.remote.firebaseuser.UserChangeSet
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.utils.errors.ErrorSolution
import mx.alxr.voicenotes.utils.errors.IErrorMessageResolver
import mx.alxr.voicenotes.utils.errors.Interaction
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.rx.SingleDisposable

class LanguageSelectorViewModel(
    db: AppDatabase,
    private val nav: IFeatureNavigation,
    private val userRepository: IUserRepository,
    private val remoteUserRepository: IRemoteUserRepository,
    private val recordsRepository: IRecordsRepository,
    private val errorMessageResolver: IErrorMessageResolver,
    @Suppress("unused") private val logger: ILogger
) : ViewModel() {

    private var mDisposable: Disposable? = null

    fun setSelectionFlag(tag: Any?) {
        mLiveModel.value?.apply { mLiveModel.value = copy(selectionFlag = true, tag = tag) }
    }

    private val dao = db.languageDataDAO()

    var mList: LiveData<PagedList<LanguageEntity>>? = null

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()

    init {
        mLiveModel.value = Model()
    }

    fun getModel(): LiveData<Model> {
        return mLiveModel
    }

    fun onErrorSolutionApplied() {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(solution = ErrorSolution())
    }

    override fun onCleared() {
        mDisposable?.dispose()
        super.onCleared()
    }

    fun getLiveData(
        filter: String,
        observer: Observer<PagedList<LanguageEntity>>
    ): LiveData<PagedList<LanguageEntity>> {
        mList?.removeObserver(observer)
        val list = if (filter.isEmpty()) {
            LivePagedListBuilder<Int, LanguageEntity>(dao.getAllByName(), 25).build()
        } else {
            LivePagedListBuilder<Int, LanguageEntity>(dao.getAllByNameFiltered(filter), 25).build()
        }
        mList = list
        return list
    }

    fun onSkipped() {
        mDisposable?.dispose()
        mLiveModel.value?.apply {
            if (selectionFlag) {
                nav.navigateFeature(FEATURE_BACK)
                return
            }
            mDisposable = userRepository
                .setNativeLanguageExplicitlyAsked()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(SingleDisposable<Unit>(
                    success = { nav.navigateFeature(FEATURE_WORKING) },
                    error = {}
                ))
        }
    }

    fun onLanguageSelected(language: LanguageEntity) {
        mDisposable?.dispose()
        mLiveModel.value?.apply {
            if (tag == null) {
                mDisposable = userRepository
                    .getUserSingle()
                    .flatMap { remoteUserRepository.change(UserChangeSet(it.firebaseUserId, language.map())) }
                    .flatMap { userRepository.update(it) }
                    .flatMap { Single.just(Unit) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(SingleDisposable<Unit>(
                        success = {
                            mLiveModel.value?.apply {
                                if (selectionFlag) {
                                    nav.navigateFeature(FEATURE_BACK)
                                } else {
                                    nav.navigateFeature(FEATURE_WORKING)
                                }
                            }
                        },
                        error = {
                            mLiveModel.value = mLiveModel.value!!
                                .copy(solution = errorMessageResolver.resolve(it, Interaction.Snack))
                        }
                    ))
            } else {
                mDisposable = recordsRepository
                    .setLanguage(tag as Long, language)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(SingleDisposable<Unit>(
                        success = {
                            nav.navigateFeature(FEATURE_BACK)
                        },
                        error = {
                            mLiveModel.value = mLiveModel.value!!
                                .copy(solution = errorMessageResolver.resolve(it, Interaction.Snack))
                        }
                    ))
            }
        }
    }

}

data class Model(
    val solution: ErrorSolution = ErrorSolution(),
    val selectionFlag: Boolean = false,
    val tag: Any? = null
)