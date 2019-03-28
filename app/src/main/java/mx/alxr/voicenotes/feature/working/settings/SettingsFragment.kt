package mx.alxr.voicenotes.feature.working.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import kotlinx.android.synthetic.main.fragment_settings.*
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.extensions.format
import mx.alxr.voicenotes.utils.extensions.showDualSelectorDialog
import org.koin.android.viewmodel.ext.android.viewModel

class SettingsFragment : Fragment(), Observer<Model>, View.OnClickListener, View.OnTouchListener {

    private val mViewModel: SettingsViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.getLiveModel().observe(this, this)
        setting_native_language.setOnClickListener(this)
        setting_sign_out.setOnClickListener(this)
        lock_view.setOnTouchListener(this)
    }

    override fun onChanged(model: Model?) {
        if (model == null) return
        setting_native_language
            .apply {
                text = if (model.language.isEmpty()) getString(R.string.native_language_empty)
                else format(R.string.native_language, model.language)
            }

        account_display_name?.applyText(model.displayName)
        account_provider?.applyText(
            if (model.authProvider.isEmpty()) "" else String.format(
                getString(R.string.auth_provider_format),
                model.authProvider
            )
        )
        account_email?.applyText(model.email)
        handleSignOut(model.signOut)
        handleUiLock(model.lockUi)
    }

    private fun TextView.applyText(value: String) {
        text = value
        visibility = if (value.trim().isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onClick(v: View?) {
        when (v) {
            setting_native_language -> mViewModel.onLanguageChangeSelected()
            setting_sign_out -> mViewModel.onSignOutRequested()
        }
    }

    private fun handleSignOut(signOut: Boolean) {
        if (!signOut) return
        val hasPermission = checkExternalStoragePermission()
        val message = if (hasPermission) getString(R.string.sign_out_dialog_message_with_permission)
        else getString(R.string.sign_out_dialog_message_without_permission)
        showDualSelectorDialog(
            message = message,
            positiveLabel = R.string.confirm,
            negativeLabel = R.string.cancel,
            positive = {
                mViewModel.onSignOutConfirm()
                performSignOut()
            },
            negative = {
                mViewModel.onSignOutCancel()
            }
        )
    }

    private fun performSignOut() {
        activity?.apply {
            AuthUI
                .getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    mViewModel.onSignedOut()
                }
        }
    }

    private fun checkExternalStoragePermission(): Boolean {
        activity?.apply {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        return false
    }

    private fun handleUiLock(lockUi:Boolean){
        lock_progress_bar_view.visibility = if (lockUi) View.VISIBLE else View.INVISIBLE
        lock_view.visibility = if (lockUi) View.VISIBLE else View.INVISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return true
    }

}