package mx.alxr.voicenotes.feature.selector


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_native_language_selector.*
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class NativeLanguageSelectorFragment : Fragment() {

    private val mViewModel: LanguageSelectorViewModel by viewModel()
    private val mLayoutInflater: LayoutInflater by inject()
    private val mLogger: ILogger by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLogger.with(this).add("onViewCreated").log()


        recycler_view.layoutManager = LinearLayoutManager(activity)
        val adapter = LanguageAdapter(mLayoutInflater, logger = mLogger)
        mViewModel.list.observe(
            this,
            Observer<PagedList<LanguageEntity>> { t -> adapter.submitList(t) }
        )
        recycler_view.adapter = adapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_native_language_selector, container, false)
    }


}
