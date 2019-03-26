package mx.alxr.voicenotes.db

import android.app.Activity
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mx.alxr.voicenotes.repository.language.LanguageDAO
import mx.alxr.voicenotes.repository.language.LanguageEntity
import mx.alxr.voicenotes.repository.record.RecordDAO
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.repository.user.UserDAO
import mx.alxr.voicenotes.repository.user.UserEntity
import mx.alxr.voicenotes.repository.wallet.TransactionDetails

@Database(entities = [RecordEntity::class, UserEntity::class, LanguageEntity::class, TransactionDetails::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordDataDAO(): RecordDAO

    abstract fun userDataDAO(): UserDAO

    abstract fun languageDataDAO(): LanguageDAO

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
