package mx.alxr.voicenotes.feature.recorder

import android.media.MediaRecorder
import android.net.Uri
import java.io.File

const val FILE_EXTENSION = "amr"
const val TEMP_RECORD_FILE = "temp_audio.$FILE_EXTENSION"
const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
const val OUTPUT_FORMAT = MediaRecorder.OutputFormat.AMR_WB
const val OUTPUT_ENCODER = MediaRecorder.AudioEncoder.AMR_WB
const val OUTPUT_ENCODER_NAME = "AMR_WB"
const val ENCODING_BITRATE = 32000
const val SAMPLING_RATE = 16000L
const val AUDIO_CHANNELS = 1
const val CONTENT_TYPE = "audio/amr-wb"


interface IRecorder {

    fun startRecording()

    fun stopRecording()

    fun getRecord(): File

    fun getRecordUri(): Uri

    fun shareRecord()

}