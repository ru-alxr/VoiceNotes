package mx.alxr.voicenotes.feature.player

import java.io.File

interface IPlayer {

    fun setPlayback(playback:IPlayback?)

    fun play(file:File, duration:Long, position:Int)

    fun stop()

}