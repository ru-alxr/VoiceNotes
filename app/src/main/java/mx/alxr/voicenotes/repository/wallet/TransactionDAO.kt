package mx.alxr.voicenotes.repository.wallet

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface TransactionDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: TransactionDetails)

    @Query("SELECT * FROM transactions WHERE isSynchronized = :isSynchronized ORDER BY date ASC")
    fun getTransactions(isSynchronized: Boolean): Flowable<List<TransactionDetails>>

    @Query("SELECT * FROM transactions WHERE transactionDescription = :transactionDescription ORDER BY date ASC")
    fun get(transactionDescription:String): Single<List<TransactionDetails>>

}