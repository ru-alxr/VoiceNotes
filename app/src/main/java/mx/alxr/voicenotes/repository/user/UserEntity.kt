package mx.alxr.voicenotes.repository.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: Long = 1,
    val languageName: String = "",
    val languageCode: String = "",
    val languageNameEnglish: String = "",

    val isLanguageRequested:Boolean = false,
    val isRegistrationRequested:Boolean = false,

    val firebaseUserId: String = "",
    val firebaseUserProvider:String = "",

    val displayName:String = "",
    val email:String = ""
)