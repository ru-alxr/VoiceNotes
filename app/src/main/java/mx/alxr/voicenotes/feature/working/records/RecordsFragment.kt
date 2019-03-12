package mx.alxr.voicenotes.feature.working.records

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_records.*
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class RecordsFragment : Fragment(), Observer<PagedList<RecordEntity>> {

    private val mViewModel: RecordsViewModel by viewModel()

    private val logger: ILogger by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_records, container, false)
    }

    private val mLayoutInflater: LayoutInflater by inject()
    private lateinit var mAdapter: RecordsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val manager = LinearLayoutManager(activity)
        manager.reverseLayout = true
        records_recycler_view.layoutManager = manager
        mAdapter = RecordsAdapter(logger, mLayoutInflater, mViewModel as ICallback, records_recycler_view)
        records_recycler_view.adapter = mAdapter
        LinearLayoutManager(activity).reverseLayout = true
        mViewModel.getModel().observe(this, Observer { it?.apply { onModelChange(this) } })
        mViewModel.getLiveData().observe(this, this)
    }

    private fun onModelChange(model: Model) {
        if (!::mAdapter.isInitialized) return
        mAdapter.setState(model.playingRecordCRC32, model.progress, model.state, model.isTracking)
    }

    override fun onChanged(list: PagedList<RecordEntity>?) {
        if (::mAdapter.isInitialized) {
            mAdapter.submitList(list)
            records_recycler_view.post { records_recycler_view.smoothScrollToPosition(0) }
        }
    }

    override fun onPause() {
        super.onPause()
        mViewModel.pauseIfPlaying()
    }

}