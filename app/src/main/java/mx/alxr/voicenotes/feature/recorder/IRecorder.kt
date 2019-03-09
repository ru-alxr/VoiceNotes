package mx.alxr.voicenotes.feature.recorder

import java.io.File

interface IRecorder {

    fun startRecording()

    fun stopRecording()

    fun getRecord(): File

}