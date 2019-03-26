package mx.alxr.voicenotes.feature.recognizer

import io.reactivex.Single
import mx.alxr.voicenotes.repository.record.RecordEntity

interface IRecognizer {

    fun prepareArgs(entity:RecordEntity):Single<TranscriptionArgs>

    fun recognize(args:TranscriptionArgs):Single<Unit>

}