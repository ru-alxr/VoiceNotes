package mx.alxr.voicenotes.repository.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
class UserEntity(@PrimaryKey val id:Long,
                 val language:String,
                 val isAsked:Boolean) :IUser{

    override fun isNativeLanguageDefined(): Boolean {
        return !language.isEmpty()
    }

    override fun getNativeLanguage(): String? {
        return language
    }

    override fun isNativeLanguageExplicitlyAsked(): Boolean {
        return isAsked
    }

}