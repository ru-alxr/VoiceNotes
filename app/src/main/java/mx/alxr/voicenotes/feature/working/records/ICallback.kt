package mx.alxr.voicenotes.feature.working.records

import mx.alxr.voicenotes.repository.record.RecordEntity

interface ICallback {
    fun onPlayButtonClick(entity: RecordEntity)
    fun onSeekBarChange(position:Int)
    fun onStartTrackingTouch()
    fun onStopTrackingTouch()
}