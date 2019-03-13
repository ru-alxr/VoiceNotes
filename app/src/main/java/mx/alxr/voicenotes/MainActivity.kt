package mx.alxr.voicenotes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import mx.alxr.voicenotes.feature.*
import mx.alxr.voicenotes.utils.extensions.hideSoftKeyboard
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

const val PAYLOAD_1 = "PAYLOAD_1"

class MainActivity : AppCompatActivity() {

    private lateinit var mNavController: NavController
    private val mViewModel: MainViewModel by viewModel()
    private val mLogger: ILogger by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mNavController = Navigation
            .findNavController(
                this@MainActivity,
                R.id.nav_host_fragment
            )
        mNavController.setGraph(R.navigation.app_navigation)
        mViewModel
            .getFeature()
            .observe(
                this,
                Observer<UserState> {
                    val args: Bundle = it?.create() ?: return@Observer
                    mLogger.with(this).add("Feature ${it.feature} ${it.args}").log()
                    when (it.feature) {
                        FEATURE_INIT -> {
                        }
                        FEATURE_PRELOAD -> if (it.args is Boolean) {
                            mNavController.navigate(R.id.action_preload_and_select_language, args)
                        } else {
                            mNavController.navigate(R.id.action_to_preload, args)
                        }
                        FEATURE_WORKING -> mNavController.navigate(R.id.action_to_work, args)
                        FEATURE_SELECT_NATIVE_LANGUAGE -> {
                            if (it.args is Boolean) {
                                mNavController.popBackStack()
                                mNavController.navigate(R.id.action_select_language, args)
                            } else {
                                mNavController.navigate(R.id.action_to_language_selector, args)
                            }
                        }
                        FEATURE_BACK -> mNavController.popBackStack()
                    }
                }
            )

    }

    override fun onResume() {
        super.onResume()
        hideSoftKeyboard()
    }

}

fun UserState.create(): Bundle {
    return Bundle()
        .apply {
            when (args) {
                is Long -> putLong(PAYLOAD_1, args)
                is String -> putString(PAYLOAD_1, args)
                is Boolean -> putBoolean(PAYLOAD_1, args)
            }
        }
}