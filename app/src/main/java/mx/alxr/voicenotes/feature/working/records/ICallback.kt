package mx.alxr.voicenotes.feature.working.records

import mx.alxr.voicenotes.repository.record.RecordEntity

interface ICallback {
    fun onPlayButtonClick(entity: RecordEntity)
    fun onStartTrackingTouch()
    fun onStopTrackingTouch()
    fun onPaused()
    fun onStopped()

    fun requestShare(entity: RecordEntity)

    fun requestGetTranscription(entity: RecordEntity)

    fun requestSynchronize(entity: RecordEntity)
    fun requestLanguageChange(recordEntity: RecordEntity)
    fun requestDeleteRecord(recordEntity: RecordEntity)

}