package mx.alxr.voicenotes.repository.config

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import io.reactivex.Single
import io.reactivex.SingleEmitter
import java.util.concurrent.Executors

const val RAW_LANGUAGES = "raw_google_cloud_speech_recognition_languages"
const val RAW_CREDENTIALS = "raw_google_cloud_credentials"
const val SYNCHRONOUS_DURATION = "google_cloud_speech_synchronous_recognition"

class ConfigRepository(
    private val config: FirebaseRemoteConfig,
    private val seconds: Long
) : IConfigRepository {

    override fun getLanguages(): Single<String> {
        val executor = Executors.newSingleThreadExecutor()
        return Single
            .create { emitter ->
                config
                    .fetch(seconds)
                    .addOnSuccessListener(executor, OnSuccessListener<Void> {
                        emitValue(
                            RAW_LANGUAGES,
                            this@ConfigRepository::extractString,
                            emitter
                        )
                    })
                    .addOnFailureListener(executor, OnFailureListener { e ->
                        onFailure(emitter, e)
                    })
            }
    }

    override fun getSynchronousDurationMillis(): Single<Long> {
        val executor = Executors.newSingleThreadExecutor()
        return Single
            .create { emitter ->
                config
                    .fetch(seconds)
                    .addOnSuccessListener(executor, OnSuccessListener<Void> {
                        emitValue(
                            SYNCHRONOUS_DURATION,
                            this@ConfigRepository::extractLong,
                            emitter
                        )
                    })
                    .addOnFailureListener(executor, OnFailureListener { e ->
                        onFailure(emitter, e)
                    })
            }
    }

    override fun getServiceCredentials(): Single<String> {
        val executor = Executors.newSingleThreadExecutor()
        return Single
            .create { emitter ->
                config
                    .fetch(seconds)
                    .addOnSuccessListener(executor, OnSuccessListener<Void> {
                        emitValue(
                            RAW_CREDENTIALS,
                            this@ConfigRepository::extractString,
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