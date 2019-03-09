package mx.alxr.voicenotes.feature.recorder

import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.lang.Exception

class Recorder(private val context: Context) : IRecorder {

    companion object {
        const val TEMP_RECORD_FILE = "temp_audio.mp4"
        const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        const val OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4
        const val OUTPUT_ENCODER = MediaRecorder.AudioEncoder.AAC
        const val ENCODING_BITRATE = 32000
        const val SAMPLING_RATE = 16000
        const val AUDIO_CHANNELS = 1
    }

    private var mMediaRecorder: MediaRecorder? = null

    override fun startRecording() {
        val directory: File = context.filesDir
        val file = File(directory, TEMP_RECORD_FILE)
        if (file.exists()) file.delete()
        file.createNewFile()
        val mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(AUDIO_SOURCE);
        mediaRecorder.setOutputFormat(OUTPUT_FORMAT);
        mediaRecorder.setAudioEncoder(OUTPUT_ENCODER);
        mediaRecorder.setAudioEncodingBitRate(ENCODING_BITRATE);
        mediaRecorder.setAudioSamplingRate(SAMPLING_RATE);
        mediaRecorder.setAudioChannels(AUDIO_CHANNELS);
        mediaRecorder.setOutputFile(file.absolutePath);
        mediaRecorder.prepare();
        mediaRecorder.start();
        mMediaRecorder = mediaRecorder
    }

    override fun stopRecording() {
        mMediaRecorder?.apply {
            try{
                stop()
            }catch (e:Exception){
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

}