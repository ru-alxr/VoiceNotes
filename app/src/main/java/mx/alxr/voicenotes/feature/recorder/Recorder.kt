package mx.alxr.voicenotes.feature.recorder

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class Recorder(private val context: Context) : IRecorder {

    private var mMediaRecorder: MediaRecorder? = null

    override fun startRecording() {
        val directory: File = context.filesDir
        val file = File(directory, TEMP_RECORD_FILE)
        if (file.exists()) file.delete()
        file.createNewFile()
        val mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(AUDIO_SOURCE)
        mediaRecorder.setOutputFormat(OUTPUT_FORMAT)
        mediaRecorder.setAudioEncoder(OUTPUT_ENCODER)
        mediaRecorder.setAudioEncodingBitRate(ENCODING_BITRATE)
        mediaRecorder.setAudioSamplingRate(SAMPLING_RATE)
        mediaRecorder.setAudioChannels(AUDIO_CHANNELS)
        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.prepare()
        mediaRecorder.start()
        stopRecording()
        mMediaRecorder = mediaRecorder
    }

    override fun stopRecording() {
        mMediaRecorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            release()
        }
        mMediaRecorder = null
    }

    override fun getRecord(): File {
        val directory: File = context.filesDir
        return File(directory, TEMP_RECORD_FILE)
    }

    override fun getRecordUri(): Uri {
        return FileProvider
            .getUriForFile(
                context,
                "${context.packageName}.provider",
                getRecord()
            )
    }

    override fun shareRecord() {
        try {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/*"
            sharingIntent.putExtra(Intent.EXTRA_STREAM, getRecordUri())
            context.startActivity(Intent.createChooser(sharingIntent, "SHARE VIA..."))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}