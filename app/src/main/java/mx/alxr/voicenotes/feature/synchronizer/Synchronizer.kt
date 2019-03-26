package mx.alxr.voicenotes.feature.synchronizer

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.*
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.feature.recorder.CONTENT_TYPE
import mx.alxr.voicenotes.feature.recorder.OUTPUT_ENCODER_NAME
import mx.alxr.voicenotes.feature.recorder.SAMPLING_RATE
import mx.alxr.voicenotes.repository.record.DIRECTORY_NAME
import mx.alxr.voicenotes.repository.record.IRecordsRepository
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.repository.record.toRemoteObject
import mx.alxr.voicenotes.utils.errors.ProjectException
import mx.alxr.voicenotes.utils.extensions.crc32
import mx.alxr.voicenotes.utils.extensions.md5Hash
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.rx.FlowableDisposable
import mx.alxr.voicenotes.utils.rx.SingleDisposable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashMap

class Synchronizer(
    private val firestore: FirebaseFirestore,
    private val recordsRepository: IRecordsRepository,
    private val extension: String,
    storage: FirebaseStorage,
    @Suppress("unused") private val logger: ILogger
) : ISynchronizer {

    private val mStorageRef = storage.reference
    private var mDisposable: Disposable? = null
    private var mPerformDisposable: Disposable? = null

    private val downloadFiles: MutableMap<String, FileDownloadTask> = HashMap()

    override fun onStart() {
        logger.with(this@Synchronizer).add("onStart").log()
        mDisposable?.dispose()
        mPerformDisposable?.dispose()
        mDisposable = recordsRepository
            .getAll(false)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribeWith(FlowableDisposable<List<RecordEntity>>(
                next = { performWith(LinkedList(it)) },
                error = {
                    it.printStackTrace()
                }
            ))
    }

    override fun onStop() {
        logger.with(this@Synchronizer).add("onStop").log()
        mDisposable?.dispose()
        mPerformDisposable?.dispose()
    }

    private fun fetchFile(entity: RecordEntity): Single<File> {
        return Single
            .create { emitter: SingleEmitter<File> -> performDownload(entity, emitter) }
    }

    private fun getTask(entity: RecordEntity): FileDownloadTask? {
        return downloadFiles[entity.uniqueId]
    }

    private fun cancelTask(entity: RecordEntity) {
        val task = getTask(entity)
        task?.cancel()
        removeTask(entity)
    }

    private fun removeTask(entity: RecordEntity) {
        downloadFiles.remove(entity.uniqueId)
    }

    private fun holdTask(entity: RecordEntity, task: FileDownloadTask) {
        downloadFiles[entity.uniqueId] = task
    }

    private fun performDownload(entity: RecordEntity, emitter: SingleEmitter<File>) {
        cancelTask(entity)
        val userFolderReference = mStorageRef.child(entity.userId)
        val fileReference = userFolderReference.child(entity.fileName)
        val localFolder: File = getDirectory()
        val localFile = File(localFolder, entity.fileName)
        val executor = Executors.newSingleThreadExecutor()
        val task = fileReference.getFile(localFile)
        task
            .addOnSuccessListener(executor, OnSuccessListener {
                val check = localFile.crc32() == entity.crc32
                removeTask(entity)
                if (check) {
                    recordsRepository.insert(entity.copy(isFileDownloaded = true))
                    emitter.onSuccess(localFile)
                } else {
                    onFailure(emitter, ProjectException(R.string.error_file_download))
                }
            })
            .addOnFailureListener(executor, OnFailureListener {
                removeTask(entity)
                onFailure(emitter, it)
            })
        holdTask(entity, task)
    }

    private fun performWith(queue: LinkedList<RecordEntity>) {
        if (queue.isEmpty()) return
        mPerformDisposable?.dispose()
        val single: Single<RemoteRecord> = Single.create { emitter -> syncRecord(queue, emitter) }
        mPerformDisposable = single
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnSuccess {
                it?.apply {
                    if (it.delete) {
                        logger.with(this@Synchronizer).add("performWith: doOnSuccess DELETE ${it.fileName}").log()
                        recordsRepository.delete(uniqueId)
                        return@doOnSuccess
                    }
                    val localFolder: File = getDirectory()
                    val localFile = File(localFolder, fileName)
                    val entity = RecordEntity(
                        fileName = fileName,
                        date = date,
                        crc32 = crc32,
                        duration = duration,
                        transcription = transcription,
                        isTranscribed = isTranscribed,
                        isSynchronized = true,
                        languageCode = languageCode,
                        userId = uid,
                        isFileUploaded = true,
                        isFileDownloaded = localFile.exists(),
                        uniqueId = uniqueId,
                        encoding = encoding,
                        sampleRateHertz = sampleRateHertz,
                        remoteFileUri = remoteFileUri
                    )
                    logger.with(this@Synchronizer).add("performWith: doOnSuccess ${entity.fileName}").log()
                    recordsRepository.insert(entity)
                }
            }
            .subscribeWith(SingleDisposable<RemoteRecord>(
                success = {
                    queue.poll()
                    if (queue.isEmpty()) return@SingleDisposable
                    performWith(queue)
                },
                error = {
                    //todo
                    // next attempt when Activity#onStart will be triggered

                    it.printStackTrace()

                }
            ))
    }

    private fun syncRecord(queue: LinkedList<RecordEntity>, emitter: SingleEmitter<RemoteRecord>) {
        if (queue.isEmpty()) {
            return
        }
        val entity = queue.first
        if (entity == null) {
            logger.with(this@Synchronizer).add("syncRecord: Nothing to sync.. BUG!").log()
            emitter.onError(NullPointerException("No records"))
            return
        }
        logger.with(this@Synchronizer).add("syncRecord ${entity.fileName}").log()
        if (entity.isDeleted) {
            performDelete(entity, emitter)
            return
        }
        if (entity.isFileUploaded) {
            onPostFileUploadSuccess(entity, emitter)
            return
        }
        val userFolderReference = mStorageRef.child(entity.userId)
        val fileReference = userFolderReference.child(entity.fileName)
        val localFolder: File = getDirectory()
        val localFile = File(localFolder, entity.fileName)
        val localFileUri = Uri.fromFile(localFile)
        val controlHash = localFile.md5Hash()
        val executor = Executors.newSingleThreadExecutor()
        val metadata = StorageMetadata
            .Builder()
            .setContentType(CONTENT_TYPE)
            .build()
        fileReference
            .putFile(localFileUri, metadata)
            .addOnSuccessListener(executor, OnSuccessListener {
                logger.with(this@Synchronizer).add("upload file ${entity.fileName} success").log()
                onFileUploadSuccess(entity, it, controlHash, emitter)
            })
            .addOnFailureListener(executor, OnFailureListener {
                logger.with(this@Synchronizer).add("upload file ${entity.fileName} fail $it").log()
                onFailure(emitter, it)
            })
    }

    private fun onFileUploadSuccess(
        entity: RecordEntity,
        fileSnapshot: UploadTask.TaskSnapshot?,
        controlHash: String,
        emitter: SingleEmitter<RemoteRecord>
    ) {
        val remoteHash: String? = fileSnapshot?.metadata?.md5Hash
        if (remoteHash == null) {
            onFailure(emitter, RuntimeException("Wrong control hash NULL"))
            return
        }
        if (remoteHash != controlHash) {
            onFailure(
                emitter,
                RuntimeException("Wrong control hash $controlHash vs $remoteHash")
            )
            return
        }
        val metadata: StorageMetadata? = fileSnapshot.metadata
        if (metadata == null) {
            onFailure(
                emitter,
                RuntimeException("No metadata for ${entity.fileName}")
            )
            return
        }
        val reference: StorageReference? = metadata.reference
        if (reference == null) {
            onFailure(
                emitter,
                RuntimeException("No reference for ${entity.fileName}")
            )
            return
        }
        logger.with(this).add("onFileUploadSuccess $reference").log()
        recordsRepository.insert(
            entity.copy(
                isFileUploaded = true,
                remoteFileUri = reference.toString()
            )
        )
        onPostFileUploadSuccess(entity, emitter)
    }

    private fun onPostFileUploadSuccess(
        entity: RecordEntity,
        emitter: SingleEmitter<RemoteRecord>
    ) {
        val executor = Executors.newSingleThreadExecutor()
        getRef(entity.userId, entity.fileName)
            .get()
            .addOnSuccessListener(executor, OnSuccessListener {
                it.apply {
                    if (emitter.isDisposed) return@OnSuccessListener
                    try {
                        if (!it.exists()) {
                            createRecord(entity, emitter)
                            return@OnSuccessListener
                        }
                        updateRemoteRecord(entity, emitter)
                    } catch (e: Exception) {
                        //todo: implement stack trace logger with ILogger!!!
                        e.printStackTrace()
                        onFailure(emitter, NullPointerException("No record"))
                    }
                }
            })
            .addOnFailureListener(executor, OnFailureListener { onFailure(emitter, it) })
    }

    private fun updateRemoteRecord(entity: RecordEntity, emitter: SingleEmitter<RemoteRecord>) {
        val recordReference = getRef(entity.userId, entity.fileName)
        logger.with(this@Synchronizer).add("updateRemoteRecord ${entity.fileName}").log()
        val executor = Executors.newSingleThreadExecutor()
        firestore
            .runTransaction { transaction ->
                transaction
                    .update(
                        recordReference,
                        entity.getMap()
                    )
                null
            }
            .addOnSuccessListener(executor, OnSuccessListener {
                logger.with(this@Synchronizer).add("updateRemoteRecord success").log()
                updateLocalRecord(entity.userId, entity.fileName, emitter)
            })
            .addOnFailureListener(executor, OnFailureListener {
                logger.with(this@Synchronizer).add("updateRemoteRecord fail").log()
                onFailure(emitter, it)
            })
    }

    private fun createRecord(entity: RecordEntity, emitter: SingleEmitter<RemoteRecord>) {
        val record = entity.getMap()
        logger.with(this@Synchronizer).add("createRecord $record").log()
        val executor = Executors.newSingleThreadExecutor()
        firestore
            .collection("users")
            .document(entity.userId)
            .collection("records")
            .document(entity.fileName)
            .set(record)
            .addOnSuccessListener(executor, OnSuccessListener {
                logger.with(this@Synchronizer).add("createRecord success").log()
                updateLocalRecord(entity.userId, entity.fileName, emitter)
            })
            .addOnFailureListener(executor, OnFailureListener {
                logger.with(this@Synchronizer).add("createRecord fail").log()
                it.printStackTrace()
                onFailure(emitter, it)
            })
    }

    private fun updateLocalRecord(userId: String, fileName: String, emitter: SingleEmitter<RemoteRecord>) {
        val recordReference = getRef(userId, fileName)
        val executor = Executors.newSingleThreadExecutor()
        recordReference
            .get()
            .addOnSuccessListener(executor, OnSuccessListener {
                it.apply {
                    if (emitter.isDisposed) return@OnSuccessListener
                    try {
                        if (!it.exists()) {
                            logger.with(this@Synchronizer).add("updateLocalRecord fail").log()
                            onFailure(emitter, NullPointerException("No record"))
                            return@OnSuccessListener
                        }
                        it.data!!.apply {
                            val record = toRemoteObject()
                            logger.with(this@Synchronizer).add("updateLocalRecord preparation with $record").log()
                            emitter.onSuccess(record)
                        }
                    } catch (e: Exception) {
                        logger.with(this@Synchronizer).add("updateLocalRecord preparation fail $e").log()
                        e.printStackTrace()
                        onFailure(emitter, NullPointerException("No record"))
                    }
                }
            })
            .addOnFailureListener(executor, OnFailureListener {
                logger.with(this@Synchronizer).add("updateLocalRecord fail $it").log()
                onFailure(emitter, it)
            })
    }

    private fun <T> onFailure(emitter: SingleEmitter<T>, e: Throwable) {
        if (emitter.isDisposed) return
        logger.with(this@Synchronizer).add("onFailure $e").log()
        emitter.onError(e)
    }

    private fun getRef(userId: String, fileName: String): DocumentReference {
        return firestore
            .collection("users")
            .document(userId)
            .collection("records")
            .document(fileName)
    }

    private val format: SimpleDateFormat by lazy { SimpleDateFormat(DATE_PATTERN, Locale.US) }

    override fun storeFile(file: File, languageCode: String): Single<Unit> {
        return Single
            .fromCallable {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(file.absolutePath)
                val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationLong = try {
                    duration.toLong()
                } catch (e: java.lang.Exception) {
                    throw ProjectException(R.string.store_file_error)
                }
                val directory = getDirectory()
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
                AudioFile(
                    name = name,
                    crc32 = crc32Copy,
                    duration = durationLong,
                    date = createdAt,
                    language = languageCode,
                    uniqueId = UUID.randomUUID().toString(),
                    encoding = OUTPUT_ENCODER_NAME,
                    sampleRateHertz = SAMPLING_RATE
                )
            }
            .flatMap { recordsRepository.insert(it) }
            .subscribeOn(Schedulers.io())
    }

    override fun getFile(entity: RecordEntity): Single<File> {
        return checkIfFileIsPresentLocally(entity.fileName, entity.crc32)
            .flatMap {
                if (it) {
                    val directory = getDirectory()
                    val target = File(directory, entity.fileName)
                    if (!entity.isFileDownloaded) recordsRepository.insert(entity.copy(isFileDownloaded = true))
                    Single.just(target)
                } else {
                    fetchFile(entity)
                }
            }
            .flatMap {
                if (entity.crc32 != it.crc32()) throw ProjectException(R.string.fetch_file_error_crc32)
                Single.just(it)
            }
    }

    private fun checkIfFileIsPresentLocally(name: String, crc32: Long): Single<Boolean> {
        return Single
            .fromCallable {
                val directory = getDirectory()
                val target = File(directory, name)
                var result: Boolean = target.exists()
                val currentCrc32 = target.crc32()
                if (result) result = crc32 == currentCrc32
                result
            }
    }

    @Throws(ProjectException::class)
    override fun getDirectory(): File {
        val directory = File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        if (!directory.exists()) throw ProjectException(R.string.store_file_error_no_directory)
        return directory
    }

    private fun performDelete(entity: RecordEntity, emitter: SingleEmitter<RemoteRecord>) {
        val localFolder: File = getDirectory()
        val localFile = File(localFolder, entity.fileName)
        if (localFile.exists()) {
            val result = localFile.delete()
            logger.with(this@Synchronizer)
                .add("local file ${entity.fileName} delete: ${if (result) "success" else "fail"}").log()
        } else {
            logger.with(this@Synchronizer).add("local file ${entity.fileName} is absent on device").log()
        }
        val ref = getRef(entity.userId, entity.fileName)
        val executor = Executors.newSingleThreadExecutor()
        ref
            .delete()
            .addOnSuccessListener(executor, OnSuccessListener {
                logger.with(this@Synchronizer)
                    .add("performDelete remote record ${entity.fileName} :OnSuccess ").log()
                performDeleteRemoteFile(entity, emitter)
            })
            .addOnFailureListener(executor, OnFailureListener {
                logger.with(this@Synchronizer)
                    .add("performDelete remote record ${entity.fileName} :OnFailure ").log()
                it.printStackTrace()
                onFailure(emitter, it)
            }
            )
    }

    private fun performDeleteRemoteFile(entity: RecordEntity, emitter: SingleEmitter<RemoteRecord>) {
        val userFolderReference = mStorageRef.child(entity.userId)
        val fileReference = userFolderReference.child(entity.fileName)
        val executor = Executors.newSingleThreadExecutor()
        fileReference
            .delete()
            .addOnSuccessListener(executor, OnSuccessListener {
                logger.with(this@Synchronizer)
                    .add("performDelete Remote File ${entity.fileName} :OnSuccess ").log()
                entity.apply {
                    val remoteStub = RemoteRecord(
                        fileName = fileName,
                        date = date,
                        crc32 = crc32,
                        duration = duration,
                        transcription = transcription,
                        isTranscribed = isTranscribed,
                        uniqueId = uniqueId,
                        uid = userId,
                        languageCode = languageCode,
                        delete = true,
                        encoding = encoding,
                        sampleRateHertz = sampleRateHertz
                    )
                    emitter.onSuccess(remoteStub)
                }
            })
            .addOnFailureListener(executor, OnFailureListener {
                logger.with(this@Synchronizer)
                    .add("performDelete Remote File ${entity.fileName} :OnFailure ").log()
                if (it is StorageException) {
                    logger.with(this@Synchronizer)
                        .add("performDelete Remote File ${entity.fileName} :OnFailure")
                        .add("isRecoverableException=${it.isRecoverableException}")
                        .add(if (it.isRecoverableException) "I hope next time I will succeed" else "but I suspect file was removed earlier")
                        .log()
                    if (!it.isRecoverableException) {
                        entity.apply {
                            val remoteStub = RemoteRecord(
                                fileName = fileName,
                                date = date,
                                crc32 = crc32,
                                duration = duration,
                                transcription = transcription,
                                isTranscribed = isTranscribed,
                                uniqueId = uniqueId,
                                uid = userId,
                                languageCode = languageCode,
                                delete = true,
                                sampleRateHertz = sampleRateHertz,
                                encoding = encoding
                            )
                            emitter.onSuccess(remoteStub)
                        }
                        return@OnFailureListener
                    }
                }
                onFailure(emitter, it)
            })
    }

}

/**
 * representation of object stored on firestore
 */
data class RemoteRecord(
    val fileName: String,
    val date: Long,
    val crc32: Long,
    val duration: Long,
    val transcription: String,
    val isTranscribed: Boolean,
    val languageCode: String,
    val uid: String,
    val uniqueId: String,
    val delete: Boolean = false,
    val encoding: String,
    val sampleRateHertz: Long,
    val remoteFileUri:String = ""
)

/**
 * representation of just recorded file
 */
data class AudioFile(
    val name: String,
    val crc32: Long,
    val duration: Long,
    val date: Long,
    val language: String,
    val uniqueId: String,
    val encoding: String,
    val sampleRateHertz: Long
)