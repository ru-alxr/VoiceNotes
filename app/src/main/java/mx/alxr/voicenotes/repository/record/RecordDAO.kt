package mx.alxr.voicenotes.repository.record

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface RecordDAO {

    @Query("SELECT * FROM records WHERE uniqueId = :uniqueId")
    fun getRecordSingle(uniqueId: String): Single<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE uniqueId = :uniqueId LIMIT 1")
    fun getRecord(uniqueId: String): RecordEntity?

    @Insert(onConflict = REPLACE)
    fun insert(record: RecordEntity)

    @Delete
    fun delete(record: RecordEntity)

    @Query("SELECT * FROM records")
    fun getAll(): List<RecordEntity>

    @Query("DELETE FROM records")
    fun deleteAll()

    @Query("SELECT * FROM records WHERE isDeleted = :deleted ORDER BY date ASC")
    fun getAllPaged(deleted:Boolean): DataSource.Factory<Int, RecordEntity>

    @Query("SELECT * FROM records WHERE isSynchronized = :isSynchronized")
    fun getAll(isSynchronized:Boolean):Flowable<List<RecordEntity>>

}