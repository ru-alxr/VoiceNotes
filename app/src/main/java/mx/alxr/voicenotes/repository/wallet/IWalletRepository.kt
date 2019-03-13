package mx.alxr.voicenotes.repository.wallet

import io.reactivex.Single
import mx.alxr.voicenotes.feature.recognizer.TranscriptionArgs

interface IWalletRepository {

    fun getAvailableCoins(): Single<Int>

    fun checkCoins(args: TranscriptionArgs): Single<TranscriptionArgs>

}