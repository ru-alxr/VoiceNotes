package mx.alxr.voicenotes.feature.working.records

import mx.alxr.voicenotes.repository.record.RecordEntity

enum class MediaPlayerState {
    Playing,
    Stopped,
    Stopping,
    Pausing
}

data class PlaybackState(
    val mpState: MediaPlayerState = MediaPlayerState.Stopped,
    val uniqueId: String = "",
    val duration: Int = 0,
    val isTracking: Boolean = false
) {

    fun isPlaying(): Boolean {
        return mpState == MediaPlayerState.Playing
    }

    fun isSameFile(entity: RecordEntity): Boolean {
        return uniqueId == entity.uniqueId
    }

}