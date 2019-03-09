package mx.alxr.voicenotes.feature.working.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_settings.*
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.extensions.format
import org.koin.android.viewmodel.ext.android.viewModel

class SettingsFragment : Fragment(), Observer<Model>, View.OnClickListener {

    private val mViewModel: SettingsViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.getLiveModel().observe(this, this)
        setting_synchronization.setOnClickListener(this)
        setting_native_language.setOnClickListener(this)
    }

    override fun onChanged(model: Model?) {
        if (model == null) return
        setting_synchronization
            .apply {
                text = if (model.isSynchronizationEnabled) {
                    getString(R.string.synchronization_status_enabled)
                } else {
                    getString(R.string.synchronization_status_disabled)
                }
            }

        setting_native_language
            .apply {
                text = if (model.language.isEmpty()) getString(R.string.native_language_empty)
                else format(R.string.native_language, model.language)
            }
    }

    override fun onClick(v: View?) {
        when(v){
            setting_synchronization ->{activity?.apply { Toast.makeText(this, "No impl.", Toast.LENGTH_SHORT).show() }}
            setting_native_language -> mViewModel.onLanguageChangeSelected()
        }
    }
}