package mx.alxr.voicenotes.repository.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
class UserEntity(@PrimaryKey val id:Long = 1L,
                 val language:String? = null,
                 val isAsked:Boolean = false) :IUser{

    override fun isNativeLanguageDefined(): Boolean {
        return !language.isNullOrEmpty()
    }

    override fun getNativeLanguage(): String? {
        return language
    }

    override fun isNativeLanguageExplicitlyAsked(): Boolean {
        return isAsked
    }

}