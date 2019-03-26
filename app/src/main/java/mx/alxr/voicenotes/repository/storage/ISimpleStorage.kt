package mx.alxr.voicenotes.repository.storage

interface ISimpleStorage {

    fun put(key:String, value:String)

    fun get(key:String, defValue:String?):String?

}