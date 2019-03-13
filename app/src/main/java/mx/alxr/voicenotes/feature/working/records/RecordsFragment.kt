package mx.alxr.voicenotes.feature.working.records

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_records.*
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.repository.media.IMediaStorage
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.utils.errors.ErrorSolution
import mx.alxr.voicenotes.utils.errors.Interaction
import mx.alxr.voicenotes.utils.errors.ProjectException
import mx.alxr.voicenotes.utils.extensions.getFileUri
import mx.alxr.voicenotes.utils.extensions.showTripleSelectorDialog
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File

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
        mAdapter = RecordsAdapter(logger, mLayoutInflater, mViewModel as ICallback, records_recycler_view)
        records_recycler_view.adapter = mAdapter
        LinearLayoutManager(activity).reverseLayout = true
        mViewModel.getModel().observe(this, Observer { it?.apply { onModelChange(this) } })
        mViewModel.getLiveData().observe(this, this)
    }

    private fun onModelChange(model: Model) {
        if (!::mAdapter.isInitialized) return
        mAdapter.setState(model.playingRecordCRC32, model.progress, model.state, model.isTracking)
        handleError(model.solution)
        shareRecord(model.share)
    }

    private fun handleError(solution: ErrorSolution) {
        if (solution.message.isEmpty()) return
        val message = solution.message
        val interaction = solution.interaction
        mViewModel.onErrorHandled()
        when (interaction) {
            Interaction.Snack -> activity?.apply {
                Snackbar
                    .make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                    .show()
            }
            else -> {
                throw RuntimeException("Unsupported interaction")
            }
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
        activity?.apply {
            try {
                val directory: File
                try {
                    directory = mediaStorage.getDirectory()
                } catch (e: ProjectException) {
                    Snackbar.make(findViewById(android.R.id.content), e.messageId, Snackbar.LENGTH_LONG).show()
                    return
                }
                val file = File(directory, path)
                if (!file.exists()) {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.fetch_file_error_no_local_file,
                        Snackbar.LENGTH_LONG
                    ).show()
                    return
                }
                val uri = getFileUri(file)
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/*"
                sharingIntent.putExtra(
                    android.content.Intent.EXTRA_SUBJECT,
                    String.format(getString(R.string.share_voice_file_subject), path)
                )
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.share_voice_file_extra))
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
                sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_voice_file_label)))
            } catch (e: Exception) {
                Snackbar
                    .make(findViewById(android.R.id.content), e.localizedMessage, Snackbar.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun shareTranscription(isTranscriptionReady: Boolean, transcription: String) {
        if (isTranscriptionReady) {
            activity?.apply {
                if (transcription.isEmpty()) {
                    Snackbar
                        .make(findViewById(android.R.id.content), R.string.empty_message, Snackbar.LENGTH_LONG)
                        .show()
                    return
                }
                try {
                    val sharingIntent = Intent(Intent.ACTION_SEND)
                    sharingIntent.type = "text/plain"
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_text_subject))
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, transcription)
                    startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_transcription_label)))
                } catch (e: Exception) {
                    Snackbar
                        .make(findViewById(android.R.id.content), e.localizedMessage, Snackbar.LENGTH_LONG)
                        .show()
                }
            }
        } else {
            activity?.apply {
                AlertDialog
                    .Builder(this)
                    .setView(R.layout.dialog_invitation_to_transcription_feature)
                    .show()
                    .apply {
                        findViewById<View>(R.id.ok_view)?.apply {
                            setOnClickListener { dismiss() }
                        }
                        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    }
            }
        }
    }

}