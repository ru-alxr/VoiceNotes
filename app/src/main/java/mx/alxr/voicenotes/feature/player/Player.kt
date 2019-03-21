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

const val TICK_PERIOD = 250L

class Player(private val logger: ILogger) : IPlayer, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
    MediaPlayer.OnCompletionListener {

    private lateinit var mediaPlayer: CustomMediaPlayer

    private var mPlayback: IPlayback? = null

    private var mExecutorService: ScheduledExecutorService? = null
    private var mDisposable: Disposable? = null

    override fun setPlayback(playback: IPlayback?) {
        mPlayback = playback
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        logger.with(this).add("onError $what").log()
        mediaPlayer.release()
        mPlayback?.onComplete()
        cleanSchedule()
        return false
    }

    override fun onSeekComplete(mp: MediaPlayer?) {

    }

    override fun onCompletion(mp: MediaPlayer?) {
        logger.with(this).add("onCompletion").log()
        mediaPlayer.reset()
        mPlayback?.onComplete()
        cleanSchedule()
    }

    override fun play(file: File, duration: Long, position: Int) {
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
            mediaPlayer.seekTo(position)
            mediaPlayer.start()
            mExecutorService = schedule()
        } catch (e: IOException) {
            logger.with(this).add("play error $e").log()
            mediaPlayer.release()
            mPlayback?.onComplete()
            e.printStackTrace()
            return
        }
    }

    override fun stop() {
        cleanSchedule()
        if (!::mediaPlayer.isInitialized) return
        if (mediaPlayer.isReset) return
        if (mediaPlayer.isReleased) return
        if (!mediaPlayer.isPrepared) return
        mediaPlayer.stop()
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
                emitter?.apply {
                    onNext(mediaPlayer.currentPosition)
                }
            }
        }, 0, TICK_PERIOD, TimeUnit.MILLISECONDS)
        mDisposable = getFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                mPlayback?.onProgress(it)
            }
            .subscribe()
        return service
    }

}