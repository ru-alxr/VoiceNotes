package mx.alxr.voicenotes.repository.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Flowable

@Dao
interface UserDAO {

    @Query("SELECT * FROM user WHERE id = 1")
    fun getUser(): Flowable<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: UserEntity)

}