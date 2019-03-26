package mx.alxr.voicenotes.feature.synchronizer

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Single
import io.reactivex.SingleEmitter
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.repository.record.RecordDAO
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.repository.record.toRemoteObject
import mx.alxr.voicenotes.repository.user.UserEntity
import mx.alxr.voicenotes.utils.logger.ILogger
import java.util.concurrent.Executors

class RecordsFetcher(
    db: AppDatabase,
    private val firestore: FirebaseFirestore,
    @Suppress("unused") private val logger: ILogger
) : IRecordsFetcher {

    private val recordsDao: RecordDAO = db.recordDataDAO()

    override fun fetch(entity: UserEntity): Single<Int> {
        return Single.create { emitter -> fetchData(entity.firebaseUserId, emitter) }
    }

    private fun fetchData(userId: String, emitter: SingleEmitter<Int>) {
        val ref = getRef(userId)
        val executor = Executors.newSingleThreadExecutor()
        ref
            .get()
            .addOnSuccessListener(executor, OnSuccessListener {
                it.apply {
                    if (emitter.isDisposed) return@OnSuccessListener
                    try {
                        var count = 0
                        for (doc in it.documents) {
                            doc.data?.toRemoteObject()?.apply {
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
                                    isFileDownloaded = false,
                                    uniqueId = uniqueId,
                                    encoding = encoding,
                                    sampleRateHertz = sampleRateHertz,
                                    remoteFileUri = remoteFileUri
                                )
                                count++
                                recordsDao.insert(entity)
                                logger.with(this@RecordsFetcher).add("FETCHED $entity").log()
                            }
                        }
                        emitter.onSuccess(count)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onFailure(emitter, NullPointerException("No record"))
                    }
                }
            })
            .addOnFailureListener(executor, OnFailureListener { onFailure(emitter, it) })
    }

    private fun getRef(userId: String): CollectionReference {
        return firestore
            .collection("users")
            .document(userId)
            .collection("records")
    }

    private fun <T> onFailure(emitter: SingleEmitter<T>, e: Throwable) {
        if (emitter.isDisposed) return
        logger.with(this@RecordsFetcher).add("onFailure $e").log()
        emitter.onError(e)
    }

}