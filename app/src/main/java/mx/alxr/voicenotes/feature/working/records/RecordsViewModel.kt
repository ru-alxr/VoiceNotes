package mx.alxr.voicenotes.feature.working.records

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.feature.player.IPlayback
import mx.alxr.voicenotes.feature.player.IPlayer
import mx.alxr.voicenotes.repository.media.IMediaStorage
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.utils.errors.ErrorSolution
import mx.alxr.voicenotes.utils.errors.IErrorMessageResolver
import mx.alxr.voicenotes.utils.errors.Interaction
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.rx.SingleDisposable
import java.io.File

class RecordsViewModel(
    db: AppDatabase,
    private val player: IPlayer,
    private val storage: IMediaStorage,
    private val resolver: IErrorMessageResolver,
    private val logger: ILogger
) : ViewModel(), ICallback, IPlayback {

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()
    private val dao = db.recordDataDAO()
    private var mDisposable: Disposable? = null

    init {
        mLiveModel.value = Model()
        player.setPlayback(this)
    }

    override fun onStartTrackingTouch() {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(isTracking = true)
    }

    override fun onStopTrackingTouch() {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(isTracking = false)
    }

    fun getModel(): LiveData<Model> {
        return mLiveModel
    }

    fun getLiveData(): LiveData<PagedList<RecordEntity>> {
        return LivePagedListBuilder<Int, RecordEntity>(dao.getAllPaged(), 10).build()
    }

    override fun onProgress(progress: Int) {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(progress = progress)
    }

    override fun onComplete() {
        mLiveModel.value = Model()
    }

    override fun onCleared() {
        mDisposable?.dispose()
        player.setPlayback(null)
        super.onCleared()
    }

    override fun onPlayButtonClick(entity: RecordEntity) {
        val model = mLiveModel.value ?: return
        val isSameFile = model.playingRecordCRC32 == entity.crc32
        if (isSameFile && model.state == PlaybackState.Playing) {
            player.pause()
            mLiveModel.value = model.copy(state = PlaybackState.Paused)
            return
        }
        val paused = isSameFile && model.state == PlaybackState.Paused
        if (!paused) mLiveModel.value = model.copy(state = PlaybackState.Stopped)
        mDisposable?.dispose()
        mDisposable = storage
            .getFile(entity.fileName, entity.crc32)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(
                SingleDisposable<File>(
                    success = { onFileReady(entity, it, paused) },
                    error = this::onFileError
                )
            )
    }

    override fun onSeekBarChange(position: Int) {
        player.jumpTo(position)
    }

    private fun onFileReady(entity: RecordEntity, file: File, resume: Boolean) {
        val model = mLiveModel.value ?: return
        if (resume) {
            mLiveModel.value = model.copy(state = PlaybackState.Playing, progress = player.resume(file))
        } else {
            player.play(file, entity.duration)
            mLiveModel.value =
                model.copy(playingRecordCRC32 = entity.crc32, state = PlaybackState.Playing, progress = 0)
        }
    }

    private fun onFileError(t: Throwable) {
        logger.with(this).add("onFileError ${resolver.resolve(t).message}").log()
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(playingRecordCRC32 = -1, state = PlaybackState.Stopped, solution = resolver.resolve(t, Interaction.Snack))
    }

    fun onErrorHandled(){
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(solution = ErrorSolution())
    }

    fun pauseIfPlaying() {
        val model = mLiveModel.value ?: return
        if (model.state == PlaybackState.Playing) {
            mLiveModel.value = model.copy(state = PlaybackState.Paused)
        }
        player.pause()
        player.deepPause()
    }

    override fun requestShare(entity: RecordEntity) {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model
            .copy(
                share = Share(
                    file = entity.fileName,
                    transcription = entity.transcription,
                    isTranscriptionReady = entity.isTranscribed
                )
            )
    }

    override fun requestGetTranscription(entity: RecordEntity) {

    }

    override fun requestSynchronize(entity: RecordEntity) {

    }

    fun onShareHandled(){
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(share = Share())
    }

}

data class Model(
    val playingRecordCRC32: Long = -1,
    val state: PlaybackState = PlaybackState.Stopped,
    val progress: Int = 0,
    val isTracking: Boolean = false,
    val share:Share = Share(),
    val solution: ErrorSolution = ErrorSolution()
)

data class Share(val file:String = "",
                 val transcription:String = "",
                 val isTranscriptionReady:Boolean = false
)