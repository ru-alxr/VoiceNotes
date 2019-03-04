package mx.alxr.voicenotes.repository.user

interface IUser {

    fun isNativeLanguageDefined():Boolean

    fun getNativeLanguage():String?

    fun isNativeLanguageExplicitlyAsked():Boolean

}