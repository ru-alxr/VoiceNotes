package mx.alxr.voicenotes.repository.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface UserDAO {

    @Query("SELECT * FROM user WHERE id = 1")
    fun getUser(): Flowable<UserEntity>

    @Query("SELECT * FROM user WHERE id = 1")
    fun getUserSingle(): Single<UserEntity>

    @Query("SELECT * FROM user WHERE id = 1")
    fun getUserImmediately(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: UserEntity)

}