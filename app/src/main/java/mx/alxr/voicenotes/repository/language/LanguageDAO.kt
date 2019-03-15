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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: LanguageEntity)

    @Query("SELECT * FROM languages ORDER BY position ASC")
    fun getAllByName(): DataSource.Factory<Int, LanguageEntity>

    @Query("SELECT * FROM languages WHERE name LIKE '%' || :filter || '%' OR nameEng LIKE '%' || :filter || '%' OR code LIKE '%' || :filter || '%' ORDER BY position ASC")
    fun getAllByNameFiltered(filter:String): DataSource.Factory<Int, LanguageEntity>

    @Query("SELECT COUNT(code) FROM languages")
    fun getCount(): Int

    @Query("SELECT * FROM languages WHERE code =:code")
    fun getLanguage(code:String):LanguageEntity?

}