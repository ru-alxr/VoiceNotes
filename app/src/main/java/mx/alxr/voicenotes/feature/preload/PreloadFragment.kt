package mx.alxr.voicenotes.feature.preload

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_preload.*
import mx.alxr.voicenotes.PAYLOAD_1
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.extensions.showDualSelectorDialog
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

/**
 * Loads list of supported native languages
 */
class PreloadFragment : Fragment(), Observer<Model> {

    private val mViewModel: PreloadViewModel by viewModel()
    private val mLogger: ILogger by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_preload, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLogger.with(this).add("onViewCreated").log()
        if (arguments?.get(PAYLOAD_1) is Boolean) mViewModel.setSelectionFlag()
        mViewModel.getModel().observe(this, this)
    }

    override fun onChanged(model: Model?) {
        if (model == null) return
        progress_view.visibility = if (model.isInProgress) View.VISIBLE else View.INVISIBLE
        if (model.solution.message.isEmpty()) return
        mViewModel.onErrorSolutionApplied()
        showDualSelectorDialog(
            message = model.solution.message,
            negativeLabel = R.string.preload_error_dialog_skip_preload,
            positiveLabel = R.string.preload_error_dialog_retry_preload,
            negative = { mViewModel.onSkipSelected() },
            positive = { mViewModel.onRetrySelected() }
        )
    }

}