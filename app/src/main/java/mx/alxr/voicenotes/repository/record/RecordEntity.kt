package mx.alxr.voicenotes.repository.record

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
class RecordEntity (@PrimaryKey
//todo
                    val name: String,
                    val hash: String){

    override fun toString(): String {
        return "USER $name /$hash"
    }

}