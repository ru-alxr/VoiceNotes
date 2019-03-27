package mx.alxr.voicenotes.feature.promo

import io.reactivex.Single

interface IPromoter {

    fun promoteRequest():Single<String>

}