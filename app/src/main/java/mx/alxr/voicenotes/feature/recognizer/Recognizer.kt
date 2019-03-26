package mx.alxr.voicenotes.feature.recognizer

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.reactivex.Single
import mx.alxr.voicenotes.BuildConfig
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.repository.config.IConfigRepository
import mx.alxr.voicenotes.repository.record.IRecordsRepository
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.repository.token.ITokenRepository
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.repository.wallet.IWalletRepository
import mx.alxr.voicenotes.utils.errors.ProjectException
import mx.alxr.voicenotes.utils.logger.ILogger
import okhttp3.*

class Recognizer(
    private val userRepository: IUserRepository,
    private val walletRepository: IWalletRepository,
    private val client: OkHttpClient,
    private val logger: ILogger,
    private val tokenRepository: ITokenRepository,
    private val recordsRepository: IRecordsRepository,
    private val moshi: Moshi,
    private val configRepository: IConfigRepository
) : IRecognizer {

    private val adapter: JsonAdapter<SynchronousRecognizeResult> by lazy { moshi.adapter(SynchronousRecognizeResult::class.java) }

    override fun prepareArgs(entity: RecordEntity): Single<TranscriptionArgs> {
        return checkUser()
            .flatMap { recordsRepository.getCurrent(entity) }
            .flatMap {
                val updated = it
                checkEntity(updated)
                    .map { TranscriptionArgs(entity = updated) }
                    .flatMap(walletRepository::checkCoins)
            }
    }

    override fun recognize(args: TranscriptionArgs): Single<Unit> {
        return configRepository
            .getSynchronousDurationMillis()
            .flatMap {
                if (args.entity!!.duration < it) recognizeSynchronous(args)
                else recognizeAsynchronous(args)
            }
    }

    private fun recognizeSynchronous(args: TranscriptionArgs): Single<Unit> {
        val job = args.entity!!
        return tokenRepository
            .getToken()
            .map {
                recordsRepository.insert(job.copy(isRecognizeInProgress = true))
                val url = BuildConfig.SPEECH_RECOGNITION_API_V1_ENDPOINT + "speech:recognize"
                val builder = Request
                    .Builder()
                    .url(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer $it")
                    .method("POST", createBody(args))
                val response: Response = client.newCall(builder.build()).execute()
                val rawAnswer: String = response.body()?.string() ?: ""
                val result = adapter.fromJson(rawAnswer)
                result
            }
            .flatMap { result ->
                val string: String = extractResult(result)
                recordsRepository
                    .getCurrent(job)
                    .flatMap {
                        recordsRepository.insert(
                            it.copy(
                                transcription = string,
                                isTranscribed = true,
                                isRecognizeInProgress = false,
                                isSynchronized = false
                            )
                        )
                        Single.just(Unit)
                    }
            }
            .doOnError {
                recordsRepository.insert(job.copy(isRecognizeInProgress = false))
            }
    }

    private fun extractResult(source: SynchronousRecognizeResult): String {
        val re = source.results ?: return ""
        val su = re.firstOrNull() ?: return ""
        val lt = su.alternatives ?: return ""
        val result = lt.firstOrNull() ?: return ""
        return result.transcript
    }

    private fun recognizeAsynchronous(args: TranscriptionArgs): Single<Unit> {
        return Single.fromCallable { throw ProjectException(R.string.error_feature_under_construction) }
    }

    private fun createBody(args: TranscriptionArgs): RequestBody {
        val entity = args.entity!!
        val mediaType: MediaType = MediaType.parse("application/json; charset=utf-8")!!
        return RequestBody.create(
            mediaType,
            entity.getData().toString()
        )
    }

    private fun checkEntity(entity: RecordEntity): Single<RecordEntity> {
        return Single
            .fromCallable {
                logger.with(this@Recognizer).add("checkEntity $entity").log()
                if (entity.languageCode.isEmpty()) throw ProjectException(R.string.error_record_language_required)
                if (!entity.isFileUploaded) throw ProjectException(R.string.error_file_must_be_uploaded)
                if (entity.isDeleted) throw ProjectException(R.string.error_cannot_recognize_deleted_record)
                if (entity.remoteFileUri.isEmpty()) throw ProjectException(R.string.error_file_must_be_uploaded)
                entity
            }
    }

    private fun checkUser(): Single<Unit> {
        return userRepository
            .getUserSingle()
            .flatMap {
                if (it.firebaseUserId.isEmpty()) throw ProjectException(R.string.error_registration_required)
                Single.just(Unit)
            }
    }

}