package mx.alxr.voicenotes.feature.selector

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.feature.FEATURE_BACK
import mx.alxr.voicenotes.feature.FEATURE_WORKING
import mx.alxr.voicenotes.feature.IFeatureNavigation
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.rx.SingleDisposable

class LanguageSelectorViewModel(
    db: AppDatabase,
    private val nav: IFeatureNavigation,
    private val userRepository: IUserRepository,
    private val logger:ILogger
) : ViewModel() {

    private var mDisposable: Disposable? = null
    private var selectionFlag:Boolean = false

    fun setSelectionFlag(){
        selectionFlag = true
    }

    private val dao = db.languageDataDAO()

    var mList: LiveData<PagedList<LanguageEntity>>? = null

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
        if (selectionFlag){
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

    fun onLanguageSelected(language: LanguageEntity) {
        logger.with(this).add("onLanguageSelected $selectionFlag").log()
        mDisposable?.dispose()
        mDisposable = userRepository
            .setUserNativeLanguage(language)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(SingleDisposable<Unit>(
                success = {
                    if (selectionFlag){
                        nav.navigateFeature(FEATURE_BACK)
                    }else{
                        nav.navigateFeature(FEATURE_WORKING)
                    }
                },
                error = {}
            ))
    }

}