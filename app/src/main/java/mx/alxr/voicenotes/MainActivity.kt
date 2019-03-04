package mx.alxr.voicenotes

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var mNavController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mNavController = Navigation
            .findNavController(
                this@MainActivity,
                R.id.nav_host_fragment
            )
        mNavController.setGraph(R.navigation.app_navigation)
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