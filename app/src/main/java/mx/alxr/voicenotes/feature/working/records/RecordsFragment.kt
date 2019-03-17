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
import mx.alxr.voicenotes.feature.recognizer.TranscriptionArgs
import mx.alxr.voicenotes.repository.media.IMediaStorage
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.utils.errors.*
import mx.alxr.voicenotes.utils.extensions.*
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class RecordsFragment : Fragment(), Observer<PagedList<RecordEntity>> {

    private val mViewModel: RecordsViewModel by viewModel()
    private val mediaStorage: IMediaStorage by inject()

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
        mAdapter = RecordsAdapter(logger, mLayoutInflater, mViewModel as ICallback)
        records_recycler_view.adapter = mAdapter
        LinearLayoutManager(activity).reverseLayout = true
        mViewModel.getModel().observe(this, Observer { it?.apply { onModelChange(this) } })
        mViewModel.getLiveData().observe(this, this)
    }

    private fun onModelChange(model: Model) {
        if (!::mAdapter.isInitialized) return
        mAdapter.setState(model.state)
        handleError(model.solution)
        shareRecord(model.share)
        handleRecognition(model.args)
    }

    private fun handleRecognition(args: TranscriptionArgs) {
        if (args.fileAbsolutePath.isEmpty()) return
        mViewModel.onRecognitionArgsHandled()
        activity?.offer(args) { mViewModel.onRecognitionAccepted(it) }
    }

    private fun handleError(solution: ErrorSolution) {
        if (solution.message.isEmpty()) return
        mViewModel.onErrorHandled()
        when (solution.interaction) {
            Interaction.Snack -> activity?.shackBar(solution.message)
            Interaction.Alert -> {
                logger.with(this).add("handleError $solution").log()
                activity?.alertRequiredData(
                    solution.message
                ) {
                    when(solution.resolutionRequired){
                        REQUIRED_USER_REGISTRATION -> mViewModel.onRegistrationSelected()
                        REQUIRED_RECORD_LANGUAGE_CODE -> mViewModel.onLanguageSelectorSelected(solution.details)
                        REQUIRED_MORE_FUNDS -> mViewModel.onFundingSelected()
                    }
                }
            }
            else -> throw RuntimeException("Unsupported interaction")
        }
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

    private fun shareRecord(share: Share) {
        if (share.file.isEmpty()) return
        mViewModel.onShareHandled()
        showTripleSelectorDialog(
            message = R.string.share_note_dialog_message,
            firstLabel = R.string.share_audio_file,
            secondLabel = R.string.share_transcription,
            thirdLabel = R.string.share_nothing,
            first = { shareFile(share.file) },
            second = { shareTranscription(share.isTranscriptionReady, share.transcription) }
        )
    }

    private fun shareFile(path: String) {
        activity?.shareFile(path, mediaStorage)
    }

    private fun shareTranscription(isTranscriptionReady: Boolean, transcription: String) {
        activity?.shareTranscription(isTranscriptionReady, transcription)
    }

}