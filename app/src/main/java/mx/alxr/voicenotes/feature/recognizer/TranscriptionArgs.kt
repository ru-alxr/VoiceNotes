package mx.alxr.voicenotes.feature.recognizer

import mx.alxr.voicenotes.repository.record.RecordEntity

data class TranscriptionArgs(val entity: RecordEntity? = null,
                             val requiredCoins:Int = 0,
                             val availableCoins:Int = -1)