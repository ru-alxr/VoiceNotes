package mx.alxr.voicenotes.feature.synchronizer

import android.net.Uri
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.repository.media.IMediaStorage
import mx.alxr.voicenotes.repository.record.RecordDAO
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.repository.record.toRemoteObject
import mx.alxr.voicenotes.utils.extensions.md5Hash
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.rx.FlowableDisposable
import mx.alxr.voicenotes.utils.rx.SingleDisposable
import java.io.File
import java.util.*
import java.util.concurrent.Executors

class Synchronizer(
    db: AppDatabase,
    private val firestore: FirebaseFirestore,
    storage: FirebaseStorage,
    private val mediaStorage: IMediaStorage,
    @Suppress("unused") private val logger: ILogger
) : ISynchronizer {

    private val mStorageRef = storage.reference
    private val recordsDao: RecordDAO = db.recordDataDAO()
    private var mDisposable: Disposable? = null
    private var mPerformDisposable: Disposable? = null

    override fun onStart() {
        logger.with(this@Synchronizer).add("onStart").log()
        mDisposable?.dispose()
        mDisposable = recordsDao
            .getAll(false)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribeWith(FlowableDisposable<List<RecordEntity>>(
                next = { performWith(LinkedList(it)) }
            ))
    }

    override fun onStop() {
        logger.with(this@Synchronizer).add("onStop").log()
        mDisposable?.dispose()
        mPerformDisposable?.dispose()
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
                        isFileUploaded = true
                    )
                    logger.with(this@Synchronizer).add("performWith: doOnSuccess $entity").log()
                    recordsDao.insert(entity)
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
        logger.with(this@Synchronizer).add("syncRecord $entity").log()
        if (entity.isFileUploaded) {
            onPostFileUploadSuccess(entity, emitter)
            return
        }
        val userFolderReference = mStorageRef.child(entity.userId)
        val fileReference = userFolderReference.child(entity.fileName)
        val localFolder: File = mediaStorage.getDirectory()
        val localFile = File(localFolder, entity.fileName)
        val localFileUri = Uri.fromFile(localFile)
        val controlHash = localFile.md5Hash()
        val executor = Executors.newSingleThreadExecutor()
        val metadata = StorageMetadata
            .Builder()
            .setContentType("audio/aac")
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
        recordsDao.insert(entity.copy(isFileUploaded = true))
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
                        e.printStackTrace()
                        onFailure(emitter, NullPointerException("No record"))
                    }
                }
            })
            .addOnFailureListener(executor, OnFailureListener { onFailure(emitter, it) })
    }

    private fun updateRemoteRecord(entity: RecordEntity, emitter: SingleEmitter<RemoteRecord>) {
        val recordReference = getRef(entity.userId, entity.fileName)
        logger.with(this@Synchronizer).add("updateRemoteRecord $entity").log()
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
}

data class RemoteRecord(
    val fileName: String,
    val date: Long,
    val crc32: Long,
    val duration: Long,
    val transcription: String,
    val isTranscribed: Boolean,
    val languageCode: String,
    val uid: String
)