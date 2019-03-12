package mx.alxr.voicenotes.feature.recorder

import android.media.MediaRecorder
import android.net.Uri
import java.io.File

const val FILE_EXTENSION = "aac"
const val TEMP_RECORD_FILE = "temp_audio.$FILE_EXTENSION"
const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
const val OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4
const val OUTPUT_ENCODER = MediaRecorder.AudioEncoder.AAC
const val ENCODING_BITRATE = 32000
const val SAMPLING_RATE = 16000
const val AUDIO_CHANNELS = 1

interface IRecorder {

    fun startRecording()

    fun stopRecording()

    fun getRecord(): File

    fun getRecordUri(): Uri

    fun shareRecord()

}