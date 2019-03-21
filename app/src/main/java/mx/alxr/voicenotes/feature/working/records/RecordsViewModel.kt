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
import mx.alxr.voicenotes.feature.FEATURE_PRELOAD
import mx.alxr.voicenotes.feature.IFeatureNavigation
import mx.alxr.voicenotes.feature.player.IPlayback
import mx.alxr.voicenotes.feature.player.IPlayer
import mx.alxr.voicenotes.feature.recognizer.IRecognizer
import mx.alxr.voicenotes.feature.recognizer.TranscriptionArgs
import mx.alxr.voicenotes.feature.synchronizer.ISynchronizer
import mx.alxr.voicenotes.repository.record.IRecordsRepository
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.repository.record.RecordTag
import mx.alxr.voicenotes.repository.record.RecordsRepository
import mx.alxr.voicenotes.utils.errors.ErrorSolution
import mx.alxr.voicenotes.utils.errors.IErrorMessageResolver
import mx.alxr.voicenotes.utils.errors.Interaction
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.rx.SingleDisposable
import java.io.File

class RecordsViewModel(
    db: AppDatabase,
    private val player: IPlayer,
    private val synchronizer: ISynchronizer,
    private val resolver: IErrorMessageResolver,
    private val logger: ILogger,
    private val recognizer: IRecognizer,
    private val navigation: IFeatureNavigation,
    private val recordsRepository: IRecordsRepository,
    val map: MutableMap<String, Int>
) : ViewModel(), ICallback, IPlayback {

    private val mLiveModel: MutableLiveData<Model> = MutableLiveData()
    private val dao = db.recordDataDAO()
    private var mDisposable: Disposable? = null
    private var mFeatureDisposable: Disposable? = null

    private val deleteEntryMap: MutableMap<String, Disposable> = HashMap()

    init {
        mLiveModel.value = Model()
        player.setPlayback(this)
    }

    fun getModel(): LiveData<Model> {
        return mLiveModel
    }

    fun getLiveData(): LiveData<PagedList<RecordEntity>> {
        return LivePagedListBuilder<Int, RecordEntity>(dao.getAllPaged(deleted = false), 10).build()
    }

    override fun onStartTrackingTouch() {
        val model = mLiveModel.value ?: return
        val mpState = model.state.mpState
        if (mpState == MediaPlayerState.Stopped) {
            mLiveModel.value = model.copy(state = model.state.copy(isTracking = true))
        } else {
            mLiveModel.value =
                model.copy(state = model.state.copy(isTracking = true, mpState = MediaPlayerState.Pausing))
            player.stop()
        }
    }

    override fun onStopTrackingTouch() {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(state = model.state.copy(isTracking = false))
    }

    override fun onProgress(progress: Int) {
        val model = mLiveModel.value ?: return
        if (model.state.uniqueId.isEmpty()) return
        map[model.state.uniqueId] = progress
        mLiveModel.value = model.copy(progressUpdate = !model.progressUpdate)
    }

    override fun onPaused() {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(state = model.state.copy(mpState = MediaPlayerState.Stopped))
    }

    override fun onStopped() {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(state = PlaybackState())
    }

    override fun onComplete() {
        val model = mLiveModel.value ?: return
        if (model.state.uniqueId.isEmpty()) return
        map[model.state.uniqueId] = 0
        mLiveModel.value =
            model.copy(state = model.state.copy(mpState = MediaPlayerState.Stopping))
    }

    override fun onCleared() {
        mDisposable?.dispose()
        player.setPlayback(null)
        for (key in deleteEntryMap.keys) deleteEntryMap[key]?.dispose()
        deleteEntryMap.clear()
        super.onCleared()
    }

    override fun onPlayButtonClick(entity: RecordEntity) {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(requestPermissionSdCard = true, recordToPlay = entity)
    }

    fun onSdCardAccessGranted(entity: RecordEntity) {
        val model = mLiveModel.value ?: return
        model.state.apply {
            if (isPlaying()) {
                player.stop()
                if (isSameFile(entity)) {
                    mLiveModel.value = model.copy(state = copy(mpState = MediaPlayerState.Pausing))
                    return
                } else {
                    mLiveModel.value = model.copy(state = copy(mpState = MediaPlayerState.Pausing))
                }
            }
            mDisposable?.dispose()
            mDisposable = synchronizer
                .getFile(entity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                    SingleDisposable<File>(
                        success = { onFileReady(entity, it) },
                        error = this@RecordsViewModel::onFileError
                    )
                )
        }
    }

    private fun onFileReady(entity: RecordEntity, file: File) {
        val model = mLiveModel.value ?: return
        val progress: Int = map[entity.uniqueId] ?: 0
        logger.with(this@RecordsViewModel).add("onFileReady SET PROGRESS $progress").log()
        player.play(file, entity.duration, progress)
        mLiveModel.value =
            model.copy(
                state = PlaybackState(
                    duration = entity.duration.toInt(),
                    uniqueId = entity.uniqueId,
                    mpState = MediaPlayerState.Playing
                )
            )
    }

    private fun onFileError(t: Throwable) {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(
            state = PlaybackState(),
            solution = resolver.resolve(t, Interaction.Snack)
        )
    }

    fun onErrorHandled() {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(solution = ErrorSolution())
    }

    fun pauseIfPlaying() {
        val model = mLiveModel.value ?: return
        if (model.state.isPlaying()) {
            mLiveModel.value = model.copy(state = model.state.copy(mpState = MediaPlayerState.Pausing))
        }
        player.stop()
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
        mFeatureDisposable?.dispose()
        mFeatureDisposable = recognizer
            .prepareArgs(entity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(
                SingleDisposable<TranscriptionArgs>(
                    success = {
                        logger.with(this@RecordsViewModel).add("requestGetTranscription success $it").log()
                        val model = mLiveModel.value ?: return@SingleDisposable
                        mLiveModel.value = model.copy(args = it)
                    },
                    error = {
                        onPrepareRecognitionError(entity, it)
                    }
                )
            )
    }

    fun onRecognitionArgsHandled() {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(args = TranscriptionArgs())
    }

    fun onRecognitionAccepted(args: TranscriptionArgs) {
        logger.with(this).add("onRecognitionAccepted").log()
    }

    private fun onPrepareRecognitionError(entity: RecordEntity, throwable: Throwable) {
        logger.with(this@RecordsViewModel).add("onPrepareRecognitionError $throwable").log()
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(solution = resolver.resolve(throwable, Interaction.Alert, entity.getMap()))
    }

    override fun requestSynchronize(entity: RecordEntity) {
        synchronizer.onStart()
    }

    fun onShareHandled() {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(share = Share())
    }

    fun onRegistrationSelected() {

    }

    fun onLanguageSelectorSelected(details: Any) {

    }

    fun onFundingSelected() {

    }

    override fun requestDeleteRecord(recordEntity: RecordEntity) {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(recordToDelete = recordEntity)
    }

    override fun requestLanguageChange(recordEntity: RecordEntity) {
        navigation.navigateFeature(FEATURE_PRELOAD, RecordTag(recordEntity.uniqueId))
    }

    fun onPermissionRequestHandled() {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(requestPermissionSdCard = false, recordToPlay = null)
    }

    fun onDeleteRecordHandled() {
        val model = mLiveModel.value ?: return
        mLiveModel.value = model.copy(recordToDelete = null)
    }

    fun onDeleteRecordConfirm(entity: RecordEntity) {
        var disposable:Disposable? = deleteEntryMap[entity.uniqueId]
        if (disposable != null) return
        disposable = recordsRepository
            .markDeleted(entity)
            .subscribeOn(Schedulers.io())
            .subscribeWith(SingleDisposable<Unit>(
                success = {
                    deleteEntryMap.remove(entity.uniqueId)
                },
                error = {
                    deleteEntryMap.remove(entity.uniqueId)
                }
            ))
        deleteEntryMap[entity.uniqueId] = disposable
    }

}

data class Model(
    val state: PlaybackState = PlaybackState(),
    val share: Share = Share(),
    val solution: ErrorSolution = ErrorSolution(),
    val args: TranscriptionArgs = TranscriptionArgs(),
    val requestPermissionSdCard: Boolean = false,
    val recordToPlay: RecordEntity? = null,
    val progressUpdate: Boolean = false,
    val recordToDelete: RecordEntity? = null
)

data class Share(
    val file: String = "",
    val transcription: String = "",
    val isTranscriptionReady: Boolean = false
)