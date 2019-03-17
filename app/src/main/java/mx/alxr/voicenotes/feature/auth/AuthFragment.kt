package mx.alxr.voicenotes.feature.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_auth.*
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.errors.ErrorSolution
import mx.alxr.voicenotes.utils.errors.Interaction
import mx.alxr.voicenotes.utils.extensions.setupToolbar
import mx.alxr.voicenotes.utils.extensions.shackBar
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

const val RC_SIGN_IN = 100

class AuthFragment : Fragment(), View.OnClickListener, Observer<Model> {

    @Suppress("unused")
    private val mLogger: ILogger by inject()
    private val mViewModel: AuthViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_auth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar_view.setTitle(R.string.auth_title)
        (activity as? AppCompatActivity)?.apply {
            setupToolbar(toolbar_view, showTitle = true, homeAsUp = false)
        }
        auth_view.setOnClickListener(this)
        mViewModel.getModel().observe(this, this)
    }

    override fun onClick(v: View?) {
        when (v) {
            auth_view -> {
                val providers = arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build())
                startActivityForResult(
                    AuthUI
                        .getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setLogo(R.drawable.ic_cloud_computing_22)
                        .setTosAndPrivacyPolicyUrls(
                            "https://google.com",
                            "https://google.com"
                        )
                        .build(),
                    RC_SIGN_IN
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null && response != null) mViewModel.onAuthSuccess(user, response.isNewUser)
                else mViewModel.onAuthFail()
            } else {
                mViewModel.onAuthFail()
            }
        }
    }

    override fun onChanged(model: Model?) {
        if (model == null) return
        auth_view.isEnabled = !model.signOut
        handleErrorMessage(model.errorMessage)
        handleErrorSolution(model.solution)
        handleSignOut(model.signOut)
    }

    private fun handleSignOut(signOut: Boolean) {
        if (!signOut) return
        activity?.apply {
            AuthUI
                .getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    mViewModel.onSignedOut()
                }
        }
    }

    private fun handleErrorMessage(id: Int) {
        if (id == 0) return
        mViewModel.onErrorMessageApplied()
        activity?.shackBar(getString(id))
    }

    private fun handleErrorSolution(solution: ErrorSolution) {
        if (solution.message.isEmpty()) return
        mViewModel.onErrorSolutionApplied()
        when (solution.interaction) {
            Interaction.Snack -> activity?.shackBar(solution.message)
            else -> throw RuntimeException("Unknown interaction ${solution.interaction}")
        }
    }

}