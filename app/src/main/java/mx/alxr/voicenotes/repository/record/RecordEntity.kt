package mx.alxr.voicenotes.repository.record

import androidx.room.Entity
import androidx.room.PrimaryKey
import mx.alxr.voicenotes.feature.synchronizer.RemoteRecord

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey
    val fileName: String,
    val date: Long,
    val crc32: Long,
    val duration: Long,
    val transcription: String = "",
    val isTranscribed: Boolean = false,
    val isSynchronized: Boolean = false,
    val languageCode: String,
    val userId:String,

    val isFileUploaded:Boolean = false,
    val isFileDownloaded:Boolean = false
) {

    fun getMap():Map<String, Any>{
        return HashMap<String, Any>().apply {
            put("file_name", fileName)
            put("crc_32", crc32)
            put("date", date)
            put("duration", duration)
            put("transcription", transcription)
            put("is_transcribed", isTranscribed)
            put("language_code", languageCode)
            put("user_id", userId)
        }
    }
}

fun Map<String, Any>.extractCrc32():Long{
    return get("crc_32") as? Long?: -2
}

fun Map<String, Any>.extractFileName():String{
    return get("file_name") as? String?: ""
}

data class RecordTag(val crc32: Long)

fun Map<String, Any?>.toRemoteObject(): RemoteRecord{
    return RemoteRecord(
        fileName = get("file_name")!!.toString(),
        date = get("date") as Long,
        crc32 = get("crc_32") as Long,
        duration = get("duration") as Long,
        transcription = get("transcription")!!.toString(),
        isTranscribed = get("is_transcribed") as Boolean,
        languageCode = get("language_code")!!.toString(),
        uid = get("user_id")!!.toString()
    )
}