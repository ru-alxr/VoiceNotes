package mx.alxr.voicenotes

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import mx.alxr.voicenotes.feature.FEATURE_INIT
import mx.alxr.voicenotes.feature.FEATURE_PRELOAD
import mx.alxr.voicenotes.feature.FEATURE_SELECT_NATIVE_LANGUAGE
import mx.alxr.voicenotes.feature.FEATURE_WORKING
import mx.alxr.voicenotes.repository.gca.IGoogleCloudApiKeyRepository
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

const val PAYLOAD_1 = "PAYLOAD_1"

class MainActivity : AppCompatActivity() {

    private lateinit var mNavController: NavController
    private val mViewModel: MainViewModel by viewModel()
    private val mLogger: ILogger by inject()
    private val r: IGoogleCloudApiKeyRepository by inject()

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
                    when (it.feature) {
                        FEATURE_INIT -> {
                        }
                        FEATURE_PRELOAD -> mNavController.navigate(R.id.action_to_preload, args)
                        FEATURE_WORKING -> mNavController.navigate(R.id.action_to_work, args)
                        FEATURE_SELECT_NATIVE_LANGUAGE -> mNavController.navigate(R.id.action_to_language_selector, args)
                    }
                }
            )

    }

    private fun playAudio(tmp: String) {
        val player = MediaPlayer.create(this, Uri.fromFile(File(tmp)))
        player.setOnPreparedListener { mp -> mp.start() }
        player.prepareAsync()
        player.setOnCompletionListener { mp -> mp.release() }
        player.isLooping = false
    }

    private fun extractAssetsFile(filePath: String): String {
        val cacheFolder = cacheDir ?: return ""
        val file = File(cacheFolder, filePath)
        if (file.exists()) return file.absolutePath
        try {
            val assetManager = assets ?: return ""
            val inputStream: InputStream = assetManager.open(filePath)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val fos = FileOutputStream(file)
            fos.write(buffer)
            fos.close()
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

}

fun UserState.create(): Bundle {
    return Bundle()
        .apply {
            when (args) {
                is Long -> putLong(PAYLOAD_1, args)
                is String -> putString(PAYLOAD_1, args)
            }
        }
}