package mx.alxr.voicenotes.feature.recognizer

import io.reactivex.Single
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.repository.media.IMediaStorage
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.repository.wallet.IWalletRepository
import mx.alxr.voicenotes.utils.errors.ProjectException

class Recognizer(
    private val mediaStorage: IMediaStorage,
    private val userRepository: IUserRepository,
    private val walletRepository: IWalletRepository
) : IRecognizer {

    override fun prepareArgs(entity: RecordEntity): Single<TranscriptionArgs> {
        return checkUser()
            .flatMap { checkEntity(entity) }
            .flatMap {
                mediaStorage
                    .getFile(entity.fileName, entity.crc32)
                    .flatMap {
                        Single
                            .just(
                                TranscriptionArgs(
                                    fileAbsolutePath = it.absolutePath,
                                    languageCode = entity.languageCode,
                                    durationMillis = entity.duration
                                )
                            )
                    }
                    .flatMap(walletRepository::checkCoins)
            }
    }

    private fun checkEntity(entity: RecordEntity): Single<Unit> {
        return Single
            .fromCallable {
                if (entity.languageCode.isEmpty()) throw ProjectException(R.string.error_record_language_required)
                Unit
            }
    }

    private fun checkUser(): Single<Unit> {
        return userRepository
            .getUserSingle()
            .flatMap {
                if (!it.isRegistered()) throw ProjectException(R.string.error_registration_required)
                Single.just(Unit)
            }
    }

}