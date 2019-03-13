package mx.alxr.voicenotes.feature.recognizer

data class TranscriptionArgs(val fileAbsolutePath:String = "",
                             val languageCode:String = "",
                             val languageName:String = "",
                             val durationMillis:Long = 0,
                             val requiredCoins:Int = 0,
                             val availableCoins:Int = -1)