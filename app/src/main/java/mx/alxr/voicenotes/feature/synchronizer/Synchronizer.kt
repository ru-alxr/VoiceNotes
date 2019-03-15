package mx.alxr.voicenotes.feature.synchronizer

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.repository.record.RecordDAO
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.repository.record.toRemoteObject
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.rx.FlowableDisposable
import mx.alxr.voicenotes.utils.rx.SingleDisposable
import java.util.*

class Synchronizer(
    db: AppDatabase,
    private val store: FirebaseFirestore,
    @Suppress("unused") private val logger: ILogger
) : ISynchronizer {

    private val recordsDao: RecordDAO = db.recordDataDAO()
    private var mDisposable: Disposable? = null
    private var mPerformDisposable: Disposable? = null

    override fun onStart() {
        logger.with(this).add("onStart").log()
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
        logger.with(this).add("onStop").log()
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
                        userId = uid
                    )
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
        if (queue.isEmpty()){
            return
        }
        val entity = queue.first
        if (entity == null) {
            logger.with(this).add("syncRecord: Nothing to sync.. BUG!").log()
            emitter.onError(NullPointerException("No records"))
            return
        }
        logger.with(this).add("syncRecord $entity").log()
        val recordReference = getRef(entity.userId, entity.fileName)
        recordReference
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.apply {
                    if (emitter.isDisposed) return@addOnSuccessListener
                    try {
                        if (!snapshot.exists()) {
                            createRecord(entity, emitter)
                            return@addOnSuccessListener
                        }
                        updateRemoteRecord(entity, emitter)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onFailure(emitter, NullPointerException("No record"))
                    }
                }
            }
            .addOnFailureListener { exception -> onFailure(emitter, exception) }
    }

    private fun updateRemoteRecord(entity: RecordEntity, emitter: SingleEmitter<RemoteRecord>) {
        val recordReference = getRef(entity.userId, entity.fileName)
        logger.with(this).add("updateRemoteRecord $entity").log()
        store
            .runTransaction { transaction ->
                transaction
                    .update(
                        recordReference,
                        entity.getMap()
                    )
                null
            }
            .addOnSuccessListener {
                logger.with(this).add("updateRemoteRecord success").log()
                updateLocalRecord(entity.userId, entity.fileName, emitter)
            }
            .addOnFailureListener {
                logger.with(this).add("updateRemoteRecord fail").log()
                onFailure(emitter, it)
            }
    }

    private fun createRecord(entity: RecordEntity, emitter: SingleEmitter<RemoteRecord>) {
        val record = entity.getMap()
        logger.with(this).add("createRecord $record").log()
        store
            .collection("users")
            .document(entity.userId)
            .collection("records")
            .document(entity.fileName)
            .set(record)
            .addOnSuccessListener {
                logger.with(this).add("createRecord success").log()
                updateLocalRecord(entity.userId, entity.fileName, emitter)
            }
            .addOnFailureListener {
                logger.with(this).add("createRecord fail").log()
                it.printStackTrace()
                onFailure(emitter, it)
            }
    }

    private fun updateLocalRecord(userId: String, fileName: String, emitter: SingleEmitter<RemoteRecord>) {
        val recordReference = getRef(userId, fileName)
        recordReference
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.apply {
                    if (emitter.isDisposed) return@addOnSuccessListener
                    try {
                        if (!snapshot.exists()) {
                            logger.with(this).add("updateLocalRecord fail").log()
                            onFailure(emitter, NullPointerException("No record"))
                            return@addOnSuccessListener
                        }
                        snapshot.data!!.apply {
                            val record = toRemoteObject()
                            logger.with(this).add("updateLocalRecord preparation with $record").log()
                            emitter.onSuccess(record)
                        }
                    } catch (e: Exception) {
                        logger.with(this).add("updateLocalRecord preparation fail $e").log()
                        e.printStackTrace()
                        onFailure(emitter, NullPointerException("No record"))
                    }
                }
            }
            .addOnFailureListener { exception ->
                logger.with(this).add("updateLocalRecord fail $exception").log()
                onFailure(emitter, exception)
            }
    }

    private fun <T> onFailure(emitter: SingleEmitter<T>, e: Throwable) {
        if (emitter.isDisposed) return
        logger.with(this).add("onFailure $e").log()
        emitter.onError(e)
    }

    private fun getRef(userId: String, fileName: String): DocumentReference {
        return store
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