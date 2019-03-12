package mx.alxr.voicenotes.repository.media

import android.media.MediaMetadataRetriever
import android.os.Environment
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.repository.record.IRecord
import mx.alxr.voicenotes.repository.record.IRecordsRepository
import mx.alxr.voicenotes.utils.errors.ProjectException
import mx.alxr.voicenotes.utils.extensions.crc32
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MediaStorage(
    private val recordsRepository: IRecordsRepository,
    private val extension: String
) : IMediaStorage {

    private lateinit var format: SimpleDateFormat

    override fun storeFile(file: File): Single<Unit> {
        return Single
            .fromCallable {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(file.absolutePath)
                val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationLong = try {
                    duration.toLong()
                } catch (e: NumberFormatException) {
                    throw ProjectException(R.string.store_file_error)
                }
                if (!::format.isInitialized) {
                    format = SimpleDateFormat(DATE_PATTERN, Locale.US)
                }
                val directory = File(Environment.getExternalStorageDirectory(), "VoiceNotesMedia")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                if (!directory.exists()) throw ProjectException(R.string.store_file_error)
                val date = format.format(Date())

                val name = String.format(FILE_NAME_PATTERN, date, extension)
                val createdAt = file.lastModified()
                val target = File(directory, name)
                try {
                    val inputStream: InputStream = FileInputStream(file)
                    val size = inputStream.available()
                    val buffer = ByteArray(size)
                    inputStream.read(buffer)
                    inputStream.close()
                    val fos = FileOutputStream(target)
                    fos.write(buffer)
                    fos.close()
                } catch (e: Exception) {
                    throw ProjectException(R.string.store_file_error)
                }
                val crc32Original = file.crc32()
                val crc32Copy = target.crc32()
                if (crc32Copy != crc32Original) {
                    target.delete()
                    throw ProjectException(R.string.store_file_error)
                }
                RecordImp(fileName = name, crc32 = crc32Copy, recordDuration = durationLong, date = createdAt)
            }
            .flatMap { recordsRepository.insert(it) }
            .subscribeOn(Schedulers.io())
    }

    override fun getFile(name: String, crc32: Long): Single<File> {
        return Single
            .fromCallable {
                val directory = File(Environment.getExternalStorageDirectory(), "VoiceNotesMedia")
                if (!directory.exists()) throw ProjectException(R.string.fetch_file_error)
                val target = File(directory, name)
                if (!target.exists()) throw ProjectException(R.string.fetch_file_error_no_local_file)
                val currentCrc32 = target.crc32()
                if (crc32 != currentCrc32) throw ProjectException(R.string.fetch_file_error_crc32)
                target
            }
    }
}

private class RecordImp(
    val fileName: String,
    private val crc32: Long,
    private val recordDuration: Long,
    private val date: Long
) : IRecord {
    override fun getDate(): Long {
        return date
    }

    override fun getCRC32(): Long {
        return crc32
    }

    override fun getName(): String {
        return fileName
    }

    override fun getDuration(): Long {
        return recordDuration
    }

    override fun getTranscription(): String {
        return ""
    }

    override fun toString(): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(recordDuration)
        val millis = recordDuration - TimeUnit.SECONDS.toMillis(seconds)
        return "$fileName $seconds.$millis sec"
    }

}