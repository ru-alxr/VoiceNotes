package mx.alxr.voicenotes.feature.player

import android.media.MediaPlayer

class CustomMediaPlayer() : MediaPlayer() {

    var isPrepared: Boolean = false
    var isReleased: Boolean = false
    var isPaused: Boolean = false
    var isReset: Boolean = false

    init {
        setOnPreparedListener {
            isPrepared = true
            isPaused = false
            isReleased = false
            isReset = false
        }
    }

    override fun release() {
        super.release()
        isReleased = true
        isPaused = false
    }

    override fun pause() {
        super.pause()
        isPaused = true
    }

    override fun start() {
        super.start()
        isPaused = false
        isReleased = false
        isReset = false
    }

    override fun reset() {
        super.reset()
        isReset = true
        isPaused = false
    }

    fun state():String{
        return "isPrepared=$isPrepared isReleased=$isReleased isPaused=$isPaused isReset=$isReset"
    }

}