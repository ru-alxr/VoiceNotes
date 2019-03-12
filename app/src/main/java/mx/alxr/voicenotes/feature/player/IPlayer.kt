package mx.alxr.voicenotes.feature.player

import java.io.File

interface IPlayer {

    fun setPlayback(playback:IPlayback?)

    fun play(file:File, duration:Long)

    fun pause()

    fun resume(file:File):Int

    fun jumpTo(position:Int)

    fun deepPause()

}