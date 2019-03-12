package mx.alxr.voicenotes.repository.record

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey
    val fileName: String,
    val date:Long,
    val crc32: Long,
    val duration: Long,
    val transcription: String = "",
    val isTranscribed:Boolean = false,
    val isSynchronized:Boolean = false
)