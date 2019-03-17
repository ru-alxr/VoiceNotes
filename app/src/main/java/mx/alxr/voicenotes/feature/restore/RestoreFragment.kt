package mx.alxr.voicenotes.feature.restore

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_restore_records.*
import mx.alxr.voicenotes.PAYLOAD_1
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.errors.ErrorSolution
import mx.alxr.voicenotes.utils.extensions.information
import mx.alxr.voicenotes.utils.extensions.setupToolbar
import mx.alxr.voicenotes.utils.extensions.showDualSelectorDialog
import org.koin.android.viewmodel.ext.android.viewModel

class RestoreFragment : Fragment(), Observer<Model> {

    private val mViewModel: RestoreViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_restore_records, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments?.get(PAYLOAD_1) is Boolean) {
            mViewModel.setSelectionFlag()
        }
        toolbar_view.setTitle(R.string.restore_records_title)
        (activity as? AppCompatActivity)?.apply {
            setupToolbar(toolbar_view, showTitle = true, homeAsUp = false)
        }
        mViewModel.getModel().observe(this, this)
    }

    override fun onChanged(model: Model?) {
        if (model == null) return
        progress_view.visibility = if (model.isInProgress) View.VISIBLE else View.INVISIBLE
        handleError(model.solution)
        handleRestoreResult(model.restoreResult)
    }

    private fun handleRestoreResult(restoreResult: Int) {
        if (restoreResult == -1) return
        activity?.information(
            String
                .format(
                    getString(R.string.restore_records_result_message),
                    restoreResult
                )
        ) { mViewModel.onResultShown() }
    }

    private fun handleError(solution: ErrorSolution) {
        if (solution.message.isEmpty()) return
        mViewModel.onErrorSolutionApplied()
        showDualSelectorDialog(
            message = solution.message,
            negativeLabel = R.string.preload_error_dialog_skip_preload,
            positiveLabel = R.string.preload_error_dialog_retry_preload,
            negative = { mViewModel.onSkipSelected() },
            positive = { mViewModel.onRetrySelected() }
        )
    }

}