package mx.alxr.voicenotes.repository.wallet

import io.reactivex.Single
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.feature.recognizer.TranscriptionArgs
import mx.alxr.voicenotes.repository.record.RecordEntity
import mx.alxr.voicenotes.utils.errors.ProjectException
import java.util.concurrent.TimeUnit

const val COIN_DURATION_SECONDS = 15

class WalletRepository:IWalletRepository {

    private var coins:Int = 100

    override fun checkCoins(args: TranscriptionArgs): Single<TranscriptionArgs> {
        return getAvailableCoins()
            .flatMap {
                val entity:RecordEntity = args.entity!!
                val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(entity.duration + 999)
                val coinsRequired:Int = ((durationSeconds + COIN_DURATION_SECONDS - 1)/ COIN_DURATION_SECONDS).toInt()
                if (coinsRequired>it) throw ProjectException(R.string.error_no_funds)
                Single.just(args.copy(requiredCoins = coinsRequired, availableCoins = it))
            }
    }

    override fun getAvailableCoins(): Single<Int> {
        return Single.just(coins)
    }

}