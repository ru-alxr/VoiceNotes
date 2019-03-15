package mx.alxr.voicenotes.repository.remote.firebaseuser

class ProjectUser(
    val uid: String,
    val authProvider:String,
    val email:String,
    val languageCode: String,
    val languageName: String,
    val languageNameEnglish: String,
    val displayName:String
){

    override fun toString(): String {
        return "ProjectUser $displayName $uid $languageCode $languageName $languageNameEnglish $authProvider"
    }
}