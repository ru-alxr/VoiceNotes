package mx.alxr.voicenotes.repository.record

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import io.reactivex.Flowable

@Dao
interface RecordDAO {

    @Query("SELECT * FROM records WHERE name = :name")
    fun getRecord(name: String): Flowable<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE name = :name AND hash = :hash")
    fun getRecord(name: String, hash: String): Flowable<List<RecordEntity>>

    @Insert(onConflict = REPLACE)
    fun insert(record: RecordEntity)

    @Query("SELECT * FROM records")
    fun getAll(): List<RecordEntity>

    @Query("DELETE FROM records")
    fun deleteAll()

}