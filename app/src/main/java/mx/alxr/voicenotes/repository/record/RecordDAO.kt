package mx.alxr.voicenotes.repository.record

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import io.reactivex.Flowable

@Dao
interface RecordDAO {

    @Query("SELECT * FROM records WHERE fileName = :name")
    fun getRecord(name: String): Flowable<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE fileName = :name AND crc32 = :crc32")
    fun getRecord(name: String, crc32: String): Flowable<List<RecordEntity>>

    @Insert(onConflict = REPLACE)
    fun insert(record: RecordEntity)

    @Query("SELECT * FROM records")
    fun getAll(): List<RecordEntity>

    @Query("DELETE FROM records")
    fun deleteAll()

    @Query("SELECT * FROM records ORDER BY date DESC")
    fun getAllPaged(): DataSource.Factory<Int, RecordEntity>

}