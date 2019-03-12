package mx.alxr.voicenotes.repository.record

interface IRecord {

    fun getCRC32():Long
    fun getName():String
    fun getDuration():Long
    fun getTranscription():String
    fun getDate():Long

}