package mx.alxr.voicenotes.repository.language

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

}