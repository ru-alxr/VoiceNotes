package mx.alxr.voicenotes.repository.wallet

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionDetails(
    @PrimaryKey
    val transactionId:String, // UUID
    val userId: String, // users id
    val transactionDescription: String, // info about transaction (purpose and so on)
    val transactionExtraDescription: String, // reserved field for info
    val change: Long, // positive or negative change, coins
    val result: Long, // result of transaction (cumulative)
    val date: Long, // date of transaction, millis
    val isSynchronized: Boolean // flag that says if info was published
)