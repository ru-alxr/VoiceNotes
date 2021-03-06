package mx.alxr.voicenotes.repository.wallet

import io.reactivex.Single
import mx.alxr.voicenotes.feature.recognizer.TranscriptionArgs
import mx.alxr.voicenotes.repository.user.UserEntity

interface IWalletRepository {

    fun getAvailableCoins(): Single<Long>

    fun checkCoins(args: TranscriptionArgs): Single<TranscriptionArgs>

    fun newbiePromotion(isNewUser: Boolean, entity:UserEntity):Single<UserEntity>

    fun updateWallet(args:TranscriptionArgs):Single<Unit>

}