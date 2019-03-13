package mx.alxr.voicenotes.feature.player

import android.media.MediaPlayer
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import mx.alxr.voicenotes.utils.logger.ILogger
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Player(private val logger: ILogger) : IPlayer, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
    MediaPlayer.OnCompletionListener {

    private lateinit var mediaPlayer: CustomMediaPlayer
    private var mDuration: Long = 0
    private var progress: Int = 0

    private var mPlayback: IPlayback? = null

    private var mExecutorService: ScheduledExecutorService? = null
    private var mDisposable: Disposable? = null

    override fun setPlayback(playback: IPlayback?) {
        mPlayback = playback
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mediaPlayer.release()
        progress = 0
        mPlayback?.onComplete()
        cleanSchedule()
        return false
    }

    override fun onSeekComplete(mp: MediaPlayer?) {

    }

    override fun onCompletion(mp: MediaPlayer?) {
        mediaPlayer.reset()
        progress = 0
        mPlayback?.onComplete()
        cleanSchedule()
    }

    override fun play(file: File, duration: Long) {
        progress = 0
        mDuration = duration
        cleanSchedule()
        if (!::mediaPlayer.isInitialized || mediaPlayer.isReleased) {
            mediaPlayer = CustomMediaPlayer()
            mediaPlayer.setOnErrorListener(this)
            mediaPlayer.setOnSeekCompleteListener(this)
            mediaPlayer.setOnCompletionListener(this)
            mediaPlayer.isLooping = false
        }
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            mExecutorService = schedule()
        } catch (e: IOException) {
            logger.with(this).add("play error $e").log()
            mediaPlayer.release()
            mPlayback?.onProgress(0)
            e.printStackTrace()
            return
        }
    }

    override fun jumpTo(position: Int) {
        cleanSchedule()
        val paused = mediaPlayer.isPaused
        if (!paused) mediaPlayer.pause()
        val millis = (mDuration.toFloat() * position.toFloat() / 100F).toInt()
        mediaPlayer.seekTo(millis)
        progress = millis
        mPlayback?.onProgress(millis)
        if (paused) return
        mediaPlayer.start()
        mExecutorService = schedule()
    }

    override fun resume(file: File): Int {
        if (!::mediaPlayer.isInitialized || mediaPlayer.isReleased) {
            mediaPlayer = CustomMediaPlayer()
            mediaPlayer.setOnErrorListener(this)
            mediaPlayer.setOnSeekCompleteListener(this)
            mediaPlayer.setOnCompletionListener(this)
            mediaPlayer.isLooping = false
            mediaPlayer.reset()
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.prepare()
        }
        mediaPlayer.seekTo(progress)
        mPlayback?.onProgress(progress)
        mediaPlayer.start()
        mExecutorService = schedule()
        return progress
    }

    override fun pause() {
        cleanSchedule()
        if (!::mediaPlayer.isInitialized) return
        if (mediaPlayer.isPrepared) mediaPlayer.pause()
        progress = mediaPlayer.currentPosition
        mPlayback?.onProgress(progress)
    }

    override fun deepPause() {
        if (!::mediaPlayer.isInitialized) return
        mediaPlayer.release()
    }

    private fun cleanSchedule() {
        mDisposable?.dispose()
        mExecutorService?.shutdownNow()
        mExecutorService = null
    }

    private var emitter: FlowableEmitter<Int>? = null

    private fun getFlowable(): Flowable<Int> {
        return Flowable.create<Int>({ emitter = it }, BackpressureStrategy.LATEST)
    }

    private fun schedule(): ScheduledExecutorService {
        val service = Executors.newSingleThreadScheduledExecutor()
        service.scheduleAtFixedRate(object : Runnable {
            override fun run() {
                if (mediaPlayer.isReleased) return
                emitter?.apply { onNext(mediaPlayer.currentPosition) }
            }
        }, 0, 250, TimeUnit.MILLISECONDS)
        mDisposable = getFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { mPlayback?.onProgress(it) }
            .subscribe()
        return service
    }

}