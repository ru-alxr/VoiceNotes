package mx.alxr.voicenotes.repository.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: Long,
    val languageName: String,
    val languageCode: String,
    val isAsked: Boolean
) : IUser {

    override fun isNativeLanguageDefined(): Boolean {
        return !languageName.isEmpty()
    }

    override fun getNativeLanguage(): String {
        return languageName
    }

    override fun isNativeLanguageExplicitlyAsked(): Boolean {
        return isAsked
    }

    override fun getNativeLanguageCode(): String {
        return languageCode
    }

    override fun isRegistered(): Boolean {
        return false
    }

}