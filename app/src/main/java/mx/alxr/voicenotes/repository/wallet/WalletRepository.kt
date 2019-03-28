package mx.alxr.voicenotes.repository.wallet

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.feature.recognizer.TranscriptionArgs
import mx.alxr.voicenotes.repository.config.IConfigRepository
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.repository.user.UserEntity
import mx.alxr.voicenotes.utils.errors.ProjectException
import mx.alxr.voicenotes.utils.logger.ILogger
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val REMOTE_COLLECTION_NAME = "transactions"

class WalletRepository(
    private val firestore: FirebaseFirestore,
    private val config: IConfigRepository,
    private val userRepository: IUserRepository,
    private val logger: ILogger
) : IWalletRepository {

    override fun checkCoins(args: TranscriptionArgs): Single<TranscriptionArgs> {
        return Single
            .zip(config.getCoinDurationSeconds().subscribeOn(Schedulers.newThread()),
                getAvailableCoins().subscribeOn(Schedulers.newThread()),
                BiFunction<Long, Long, TranscriptionArgs> { seconds, coins ->
                    val entity: RecordEntity = args.entity!!
                    val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(entity.duration + 999)
                    val coinsRequired: Long = (durationSeconds + seconds - 1) / seconds
                    if (coins == 0L) {
                        if (coinsRequired > 1) throw ProjectException(R.string.error_absolutely_no_funds, coinsRequired)
                        throw ProjectException(R.string.error_absolutely_no_funds_one_coin_required, coinsRequired)
                    }
                    if (coinsRequired > coins) throw ProjectException(R.string.error_not_enough_funds, arrayOf(coinsRequired.toString(), coins.toString()))
                    args.copy(requiredCoins = coinsRequired, availableCoins = coins)
                }
            )
    }

    override fun getAvailableCoins(): Single<Long> {
        val firstOrNull = 1L
        return userRepository
            .getUserSingle()
            .flatMap { user -> getTransactions(user, firstOrNull) }
            .map { list -> list.firstOrNull()?.result ?: 0L }
    }

    override fun newbiePromotion(isNewUser: Boolean, entity: UserEntity): Single<UserEntity> {
        return getTransactions(entity, 1)
            .flatMap {
                if (it.isEmpty()) {
                    // no transactions yet, we can promote user
                    promoteUser(entity)

                } else {
                    // user has some transactions, we cannot promote user
                    Single.just(entity)
                }
            }
    }

    override fun updateWallet(args: TranscriptionArgs): Single<Unit> {
        return getAvailableCoins()
            .flatMap {balance ->
                val entity = args.entity!!
                val result = balance - args.requiredCoins
                val transactionDescription = entity.remoteFileUri
                val transactionExtraDescription = entity.languageCode
                val writeOff = TransactionDetails(
                    transactionId = UUID.randomUUID().toString(),
                    date = System.currentTimeMillis(),
                    userId = entity.userId,
                    change = - args.requiredCoins,
                    result = result,
                    transactionDescription = transactionDescription,
                    transactionExtraDescription = transactionExtraDescription,
                    isSynchronized = false
                )
                Single.create { emitter: SingleEmitter<Unit> -> push(entity.userId, writeOff, emitter, Unit) }
            }
    }

    private fun promoteUser(entity: UserEntity): Single<UserEntity> {
        return config
            .getInitialCoinsAmount()
            .flatMap {
                val promotion = TransactionDetails(
                    transactionId = UUID.randomUUID().toString(),
                    date = System.currentTimeMillis(),
                    userId = entity.firebaseUserId,
                    change = it.amount,
                    result = it.amount,
                    transactionDescription = it.description,
                    transactionExtraDescription = it.extraDescription,
                    isSynchronized = false
                )
                Single.create { emitter: SingleEmitter<UserEntity> -> push(entity.firebaseUserId, promotion, emitter, entity) }
            }
    }

    private fun <Type> push(userId:String,
                            transaction: TransactionDetails,
                            emitter: SingleEmitter<Type>,
                            transit: Type) {
        val executor = Executors.newSingleThreadExecutor()
        firestore
            .collection("users")
            .document(userId)
            .collection(REMOTE_COLLECTION_NAME)
            .add(transaction.toMap())
            .addOnSuccessListener(executor, OnSuccessListener {
                logger.with(this@WalletRepository).add("push transaction success").log()
                onSuccess(emitter, transit)
                // todo надо ли сохранять локально?
            })
            .addOnFailureListener(executor, OnFailureListener {
                logger.with(this@WalletRepository).add("push transaction fail").log()
                logger.with(this).e(it)
                onFailure(emitter, it)
            })
    }

    private fun getTransactions(entity: UserEntity, limit: Long): Single<List<RemoteTransaction>> {
        return Single.create { emitter: SingleEmitter<List<RemoteTransaction>> ->
            getTransactions(
                entity,
                emitter,
                limit
            )
        }
    }

    private fun getTransactions(entity: UserEntity, emitter: SingleEmitter<List<RemoteTransaction>>, limit: Long) {
        val ref = getRef(entity.firebaseUserId)
        ref
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { shot ->
                onSuccess(emitter, shot.documents.filter { it.data != null }.map { it.data!! }.map { it.toRemote() })
            }
            .addOnFailureListener {
                onFailure(emitter, it)
            }
    }

    private fun <T> onSuccess(emitter: SingleEmitter<T>, result: T) {
        if (emitter.isDisposed) return
        emitter.onSuccess(result)
    }

    private fun getRef(userId: String): CollectionReference {
        return firestore
            .collection("users")
            .document(userId)
            .collection(REMOTE_COLLECTION_NAME)
    }

    private fun <T> onFailure(emitter: SingleEmitter<T>, e: Throwable) {
        if (emitter.isDisposed) return
        logger.with(this@WalletRepository).add("onFailure $e").log()
        emitter.onError(e)
    }
}

data class RemoteTransaction(
    val transactionId: String, // UUID
    val userId: String, // users id
    val transactionDescription: String, // info about transaction (purpose and so on)
    val transactionExtraDescription: String, // reserved field for info
    val change: Long, // positive or negative change, coins
    val result: Long, // result of transaction (cumulative)
    val date: Long // date of transaction, millis
)

fun TransactionDetails.toMap(): Map<String, Any> {
    return HashMap<String, Any>().apply {
        put("transaction_id", transactionId)
        put("user_id", userId)
        put("transaction_description", transactionDescription)
        put("transaction_extra_description", transactionExtraDescription)
        put("change", change)
        put("result", result)
        put("date", date)
    }
}

fun Map<String, Any?>.toRemote(): RemoteTransaction {
    return RemoteTransaction(
        transactionId = get("transaction_id")!!.toString(),
        userId = get("user_id")!!.toString(),
        transactionDescription = get("transaction_description")!!.toString(),
        transactionExtraDescription = get("transaction_extra_description")!!.toString(),
        change = get("change") as Long,
        result = get("result") as Long,
        date = get("date") as Long
    )

}