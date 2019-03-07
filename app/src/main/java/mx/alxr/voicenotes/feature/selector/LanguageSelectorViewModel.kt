package mx.alxr.voicenotes.feature.selector

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.repository.language.LanguageEntity

class LanguageSelectorViewModel(db: AppDatabase) : ViewModel() {

    private val dao = db.languageDataDAO()

    val list: LiveData<PagedList<LanguageEntity>> =
        LivePagedListBuilder<Int, LanguageEntity>(dao.getAllByName(), 25).build()


}