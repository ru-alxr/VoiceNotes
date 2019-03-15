package mx.alxr.voicenotes.feature.player

import android.media.MediaPlayer

class CustomMediaPlayer() : MediaPlayer() {

    var isPrepared: Boolean = false
    var isReleased: Boolean = false
    var isStopped: Boolean = false
    var isPaused: Boolean = false
    var isReset: Boolean = false

    init {
        setOnPreparedListener { isPrepared = true }
    }

    override fun release() {
        super.release()
        isReleased = true
    }

    override fun stop() {
        super.stop()
        isStopped = true
    }

    override fun pause() {
        super.pause()
        isPaused = true
    }

    override fun start() {
        super.start()
        isPaused = false
        isStopped = false
    }

    override fun reset() {
        super.reset()
        isReset = true
    }

    fun state():String{
        return "isPrepared=$isPrepared isReleased=$isReleased isStopped=$isStopped isPaused=$isPaused isReset=$isReset"
    }

}