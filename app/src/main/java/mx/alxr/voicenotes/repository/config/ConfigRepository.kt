package mx.alxr.voicenotes.repository.config

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors

const val RAW_LANGUAGES = "raw_google_cloud_speech_recognition_languages"
const val RAW_CREDENTIALS = "raw_google_cloud_credentials"
const val SYNCHRONOUS_DURATION = "google_cloud_speech_synchronous_recognition"

const val INITIAL_PROMO_COINS_AMOUNT = "initial_promo_coins_amount"
const val INITIAL_PROMO_DESCRIPTION = "initial_promo_description"
const val INITIAL_PROMO_EXTRA_DESCRIPTION = "initial_promo_extra_description"

const val COIN_DURATION_SECONDS = "coin_duration_seconds"
const val SLACK_TOKEN = "slack_bot_token"

class ConfigRepository(
    private val config: FirebaseRemoteConfig,
    private val seconds: Long
) : IConfigRepository {

    override fun getLanguages(): Single<String> {
        return implString(RAW_LANGUAGES)
    }

    override fun getServiceCredentials(): Single<String> {
        return implString(RAW_CREDENTIALS)
    }

    override fun getSynchronousDurationMillis(): Single<Long> {
        return imp(SYNCHRONOUS_DURATION)
    }

    override fun getSlackToken(): Single<String> {
        return implString(SLACK_TOKEN)
    }

    override fun getInitialCoinsAmount(): Single<InitialPromo> {
        return Single
            .zip(
                imp(INITIAL_PROMO_COINS_AMOUNT).subscribeOn(Schedulers.newThread()),
                implString(INITIAL_PROMO_DESCRIPTION).subscribeOn(Schedulers.newThread()),
                implString(INITIAL_PROMO_EXTRA_DESCRIPTION).subscribeOn(Schedulers.newThread()),
                Function3<Long, String, String, InitialPromo> { amount, description, extraDescription ->
                    InitialPromo(
                        amount = amount,
                        description = description,
                        extraDescription = extraDescription
                    )
                })
    }

    override fun getCoinDurationSeconds(): Single<Long> {
        return imp(COIN_DURATION_SECONDS)
    }

    private fun implString(key: String): Single<String> {
        val executor = Executors.newSingleThreadExecutor()
        return Single
            .create { emitter ->
                config
                    .fetch(seconds)
                    .addOnSuccessListener(executor, OnSuccessListener<Void> {
                        emitValue(
                            key,
                            this@ConfigRepository::extractString,
                            emitter
                        )
                    })
                    .addOnFailureListener(executor, OnFailureListener { e ->
                        onFailure(emitter, e)
                    })
            }
    }

    private fun imp(key: String): Single<Long> {
        val executor = Executors.newSingleThreadExecutor()
        return Single
            .create { emitter ->
                config
                    .fetch(seconds)
                    .addOnSuccessListener(executor, OnSuccessListener<Void> {
                        emitValue(
                            key,
                            this@ConfigRepository::extractLong,
                            emitter
                        )
                    })
                    .addOnFailureListener(executor, OnFailureListener { e ->
                        onFailure(emitter, e)
                    })
            }
    }

    private fun extractString(value: FirebaseRemoteConfigValue): String {
        return value.asString()
    }

    private fun extractLong(value: FirebaseRemoteConfigValue): Long {
        return value.asLong()
    }

    private fun <T> emitValue(key: String, extractor: (FirebaseRemoteConfigValue) -> T, emitter: SingleEmitter<T>) {
        if (emitter.isDisposed) return
        config.activateFetched()
        val value = config.getValue(key)
        val extracted = extractor.invoke(value)
        emitter.onSuccess(extracted)
    }

    private fun <T> onFailure(emitter: SingleEmitter<T>, e: Throwable) {
        if (emitter.isDisposed) return
        emitter.onError(e)
    }

}