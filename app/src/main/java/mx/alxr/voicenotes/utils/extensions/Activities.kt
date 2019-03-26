package mx.alxr.voicenotes.utils.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.feature.recognizer.TranscriptionArgs
import mx.alxr.voicenotes.feature.synchronizer.ISynchronizer
import mx.alxr.voicenotes.utils.errors.ProjectException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

fun Activity.hideSoftKeyboard() {
    val token = (currentFocus ?: findViewById(android.R.id.content))?.windowToken ?: return
    val manager = applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    manager?.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
}

fun AppCompatActivity.setupToolbar(
    toolbar: Toolbar,
    homeAsUp: Boolean = true,
    showTitle: Boolean = true,
    indicator: Drawable? = null
) {
    setSupportActionBar(toolbar)
    if (indicator != null) {
        toolbar.navigationIcon = indicator
    }
    supportActionBar?.apply {
        setDisplayHomeAsUpEnabled(homeAsUp)
        setDisplayShowTitleEnabled(showTitle)
    }
}

fun Activity.vibrate(duration: Long) {
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator? ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(duration)
    }
}

fun Activity.goAppSettings() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    try {
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
    }

}

fun Activity.getFileUri(file: File): Uri {
    return FileProvider
        .getUriForFile(
            this,
            "$packageName.provider",
            file
        )
}

@Suppress("unused")
fun Activity.extractAssetsFile(filePath: String): String {
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

fun Activity.offer(args: TranscriptionArgs, positive: (TranscriptionArgs) -> Unit) {
    val entity = args.entity ?: return
    AlertDialog
        .Builder(this)
        .setView(R.layout.dialog_confirm_recognition_arguments)
        .show()
        .apply {
            findViewById<TextView>(R.id.file_name_view)?.apply {
                text = entity.fileName
            }

            findViewById<TextView>(R.id.duration_view)?.apply {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(entity.duration)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(entity.duration + 500L) - TimeUnit.MINUTES.toSeconds(minutes)
                text = String.format("%d:%02d", minutes, seconds)
            }

            findViewById<TextView>(R.id.cost_view)?.apply {
                text = String.format(getString(R.string.cost_coins), args.requiredCoins)
            }

            findViewById<TextView>(R.id.remaining_funds_view)?.apply {
                text = String.format(getString(R.string.remaining_coins), args.availableCoins)
            }

            findViewById<TextView>(R.id.negative)?.apply {
                setText(R.string.cancel)
                setOnClickListener {
                    dismiss()
                }
            }
            findViewById<TextView>(R.id.positive)?.apply {
                setText(R.string.confirm)
                setOnClickListener {
                    dismiss()
                    positive.invoke(args)
                }
            }
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
}

fun Activity.shareFile(path: String, synchronizer: ISynchronizer){
    try {
        val directory: File
        try {
            directory = synchronizer.getDirectory()
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

fun Activity.shareTranscription(isTranscriptionReady: Boolean, transcription: String){
    if (isTranscriptionReady) {
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
    } else {
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

fun Activity.alertRequiredData(message: String, solution: ()-> Unit){
    AlertDialog
        .Builder(this)
        .setView(R.layout.dialog_data_required)
        .show()
        .apply {
            findViewById<TextView>(R.id.required_info)?.apply {
                text = message
            }
            findViewById<TextView>(R.id.negative)?.apply {
                setText(R.string.cancel)
                setOnClickListener {
                    dismiss()
                }
            }
            findViewById<TextView>(R.id.positive)?.apply {
                setText(android.R.string.ok)
                setOnClickListener {
                    dismiss()
                    solution.invoke()
                }
            }
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
}

fun Activity.shackBar(message:String){
    Snackbar
        .make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        .show()
}

fun Activity.information(message:String, ok: ()->Unit){
    AlertDialog
        .Builder(this)
        .setView(R.layout.dialog_information)
        .setCancelable(false)
        .show()
        .apply {
            findViewById<TextView>(R.id.info)?.apply {
                text = message
            }
            findViewById<TextView>(R.id.negative)?.apply {
                setText(R.string.cancel)
                setOnClickListener {
                    dismiss()
                }
            }
            findViewById<TextView>(R.id.neutral)?.apply {
                setOnClickListener {
                    dismiss()
                    ok.invoke()
                }
            }
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
}