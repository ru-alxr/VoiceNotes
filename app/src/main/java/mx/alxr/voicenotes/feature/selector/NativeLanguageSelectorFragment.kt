package mx.alxr.voicenotes.feature.selector

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_native_language_selector.*
import mx.alxr.voicenotes.PAYLOAD_1
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.utils.extensions.hideSoftKeyboard
import mx.alxr.voicenotes.utils.extensions.setupToolbar
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

class NativeLanguageSelectorFragment : Fragment(), Observer<PagedList<LanguageEntity>>, SelectionCallback {

    private val mViewModel: LanguageSelectorViewModel by viewModel()
    private val mLayoutInflater: LayoutInflater by inject()
    private val mLogger: ILogger by inject()

    private lateinit var mAdapter: LanguageAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments?.get(PAYLOAD_1) is Boolean) mViewModel.setSelectionFlag()
        recycler_view.layoutManager = LinearLayoutManager(activity)
        mAdapter = LanguageAdapter(mLayoutInflater, logger = mLogger, callback = this)
        onQueryChange("")
        recycler_view.adapter = mAdapter

        language_filter_view
            .textChanges()
            .debounce(500L, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.toString() }
            .doOnNext { onQueryChange(it) }
            .subscribe()

        recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == SCROLL_STATE_DRAGGING) activity?.hideSoftKeyboard()
            }
        })
        toolbar_view.setTitle(R.string.lang_selector)
        (activity as? AppCompatActivity)?.apply {
            setupToolbar(toolbar_view, showTitle = true, homeAsUp = false)
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_skip, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.skip -> {
                activity?.hideSoftKeyboard()
                mViewModel.onSkipped()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onChanged(t: PagedList<LanguageEntity>?) {
        mAdapter.submitList(t)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_native_language_selector, container, false)
    }

    private fun onQueryChange(filter: String) {
        mViewModel
            .getLiveData(filter, this)
            .observe(
                this,
                this
            )
    }

    override fun onSelected(entity: LanguageEntity?) {
        entity?.apply {
            activity?.hideSoftKeyboard()
            mViewModel.onLanguageSelected(this)
        }
    }

    override fun onPause() {
        super.onPause()
        activity?.hideSoftKeyboard()
    }

}