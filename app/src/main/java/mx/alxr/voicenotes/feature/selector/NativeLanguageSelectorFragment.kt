package mx.alxr.voicenotes.feature.selector

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.TextView
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
import mx.alxr.voicenotes.PAYLOAD_2
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.utils.errors.ErrorSolution
import mx.alxr.voicenotes.utils.errors.Interaction
import mx.alxr.voicenotes.utils.extensions.hideSoftKeyboard
import mx.alxr.voicenotes.utils.extensions.setupToolbar
import mx.alxr.voicenotes.utils.extensions.shackBar
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
        if (arguments?.get(PAYLOAD_1) is Boolean) {
            mViewModel.setSelectionFlag(arguments?.get(PAYLOAD_2))
        }
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
        language_filter_view.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH){
                    activity?.hideSoftKeyboard()
                }
                return true
            }
        })
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
        mViewModel.getModel().observe(this, mModelChangeObserver)
    }

    private val mModelChangeObserver = Observer<Model> {
        if (it == null) return@Observer
        handleError(it.solution)
    }

    private fun handleError(solution: ErrorSolution) {
        if (solution.message.isEmpty()) return
        mViewModel.onErrorSolutionApplied()
        when (solution.interaction) {
            Interaction.Snack -> activity?.shackBar(solution.message)
            else -> throw RuntimeException("Unsupported interaction")
        }
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