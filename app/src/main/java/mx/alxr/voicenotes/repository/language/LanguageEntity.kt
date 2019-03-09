package mx.alxr.voicenotes.repository.language

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "languages")
data class LanguageEntity(
    @PrimaryKey val code: String,
    val name: String,
    val nameEng: String,
    val position:Int
){

    override fun toString(): String {
        return "$code = $name"
    }

}