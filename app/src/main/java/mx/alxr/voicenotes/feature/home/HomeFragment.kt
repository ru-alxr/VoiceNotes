package mx.alxr.voicenotes.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_home.*
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.extensions.format
import org.koin.android.viewmodel.ext.android.viewModel

class HomeFragment : Fragment(), Observer<Model> {

    private val mViewModel: HomeViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.getLiveModel().observe(this, this)
    }

    override fun onChanged(model: Model?) {
        if (model == null) return
        synchronization_status_view
            .apply {
                isEnabled = model.isSynchronizationEnabled
                text = if (model.isSynchronizationEnabled) {
                    getString(R.string.synchronization_status_enabled)
                } else {
                    getString(R.string.synchronization_status_disabled)
                }
            }

        native_language_view
            .apply {
                text = if (model.language.isEmpty()) getString(R.string.native_language_empty)
                else format(R.string.native_language, model.language)
            }
    }

}