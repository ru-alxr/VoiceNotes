package mx.alxr.voicenotes.db

import android.app.Activity
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mx.alxr.voicenotes.repository.record.RecordDAO
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.repository.user.UserDAO
import mx.alxr.voicenotes.repository.user.UserEntity

@Database(entities = [RecordEntity::class, UserEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordDataDAO(): RecordDAO

    abstract fun userDataDAO(): UserDAO

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase? {
            if (context is Activity) throw RuntimeException("Must not be activity context")
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    val instance = Room
                        .databaseBuilder(
                            context,
                            AppDatabase::class.java,
                            "voice.db"
                        )
                        .build()
                    INSTANCE = instance
                }
            }
            return INSTANCE
        }
    }

}
