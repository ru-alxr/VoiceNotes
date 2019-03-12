package mx.alxr.voicenotes.feature.player

interface IPlayback {

    fun onProgress(progress:Int)

    fun onComplete()

}