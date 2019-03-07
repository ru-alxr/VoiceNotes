package mx.alxr.voicenotes.repository.language

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Flowable

@Dao
interface LanguageDAO {

    @Query("SELECT * FROM languages")
    fun getAll(): Flowable<List<LanguageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(list: List<LanguageEntity>)

    @Query("SELECT * FROM languages ORDER BY position ASC")
    fun getAllByName(): DataSource.Factory<Int, LanguageEntity>

}