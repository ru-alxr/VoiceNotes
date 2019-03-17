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
    val crc32: Long = -1,
    val duration: Int = 0,
    val progress: Int = 0,
    val isTracking: Boolean = false
) {

    fun isPlaying(): Boolean {
        return mpState == MediaPlayerState.Playing
    }

    fun isStopped(): Boolean {
        return mpState == MediaPlayerState.Stopped
    }

    fun isSameFile(entity: RecordEntity): Boolean {
        return crc32 == entity.crc32
    }

}