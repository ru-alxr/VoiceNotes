package mx.alxr.voicenotes.feature.working.records

import mx.alxr.voicenotes.repository.record.RecordEntity

interface ICallback {
    fun onPlayButtonClick(entity: RecordEntity)
    fun onSeekBarChange(position:Int)
    fun onStartTrackingTouch()
    fun onStopTrackingTouch()

    fun requestShare(entity: RecordEntity)

    fun requestGetTranscription(entity: RecordEntity)

    fun requestSynchronize(entity: RecordEntity)

}