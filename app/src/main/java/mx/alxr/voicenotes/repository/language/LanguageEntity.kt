package mx.alxr.voicenotes.repository.language

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "languages")
data class LanguageEntity(
    @PrimaryKey val code: String,
    val name: String,
    val nameEng: String,
    val position: Int
) {

    override fun toString(): String {
        return "$code = $name"
    }

    fun map(): Map<String, Any> {
        return HashMap<String, String>()
            .apply {
                put("language_code", code)
                put("language_name", name)
                put("language_name_english", nameEng)
            }
    }

}