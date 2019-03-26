package mx.alxr.voicenotes.di

import android.view.LayoutInflater
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.BuildConfig
import mx.alxr.voicenotes.application.getAppLogger
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.feature.FeatureNavigation
import mx.alxr.voicenotes.feature.IFeatureNavigation
import mx.alxr.voicenotes.feature.player.IPlayer
import mx.alxr.voicenotes.feature.player.Player
import mx.alxr.voicenotes.feature.recognizer.IRecognizer
import mx.alxr.voicenotes.feature.recognizer.Recognizer
import mx.alxr.voicenotes.feature.recorder.FILE_EXTENSION
import mx.alxr.voicenotes.feature.recorder.IRecorder
import mx.alxr.voicenotes.feature.recorder.Recorder
import mx.alxr.voicenotes.feature.synchronizer.IRecordsFetcher
import mx.alxr.voicenotes.feature.synchronizer.ISynchronizer
import mx.alxr.voicenotes.feature.synchronizer.RecordsFetcher
import mx.alxr.voicenotes.feature.synchronizer.Synchronizer
import mx.alxr.voicenotes.repository.config.*
import mx.alxr.voicenotes.repository.language.ILanguageRepository
import mx.alxr.voicenotes.repository.language.LanguageRepository
import mx.alxr.voicenotes.repository.record.IRecordsRepository
import mx.alxr.voicenotes.repository.record.RecordsRepository
import mx.alxr.voicenotes.repository.remote.firebaseuser.IRemoteUserRepository
import mx.alxr.voicenotes.repository.remote.firebaseuser.RemoteUserRepository
import mx.alxr.voicenotes.repository.storage.ISimpleStorage
import mx.alxr.voicenotes.repository.storage.SimpleStorage
import mx.alxr.voicenotes.repository.token.ITokenRepository
import mx.alxr.voicenotes.repository.token.TokenRepository
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.repository.user.UserRepository
import mx.alxr.voicenotes.repository.wallet.IWalletRepository
import mx.alxr.voicenotes.repository.wallet.WalletRepository
import mx.alxr.voicenotes.utils.errors.ErrorMessageResolver
import mx.alxr.voicenotes.utils.errors.IErrorMessageResolver
import mx.alxr.voicenotes.utils.logger.ILogger
import mx.alxr.voicenotes.utils.resources.IStringResources
import mx.alxr.voicenotes.utils.resources.StringResources
import okhttp3.*
import okio.Buffer
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

val APPLICATION_MODULE = module {

    single { androidContext().getAppLogger() }

    single { LayoutInflater.from(androidContext()) }

    single { UserRepository(db = get(), logger = get()) as IUserRepository }

    single(createOnStart = true) { AppDatabase.getInstance(androidContext()) as AppDatabase }

    single { FeatureNavigation(logger = get()) as IFeatureNavigation }

    single { StringResources(androidContext()) as IStringResources }

    single { LanguageRepository(configRepo = get(), db = get(), logger = get()) as ILanguageRepository }

    single { ErrorMessageResolver(resources = get()) as IErrorMessageResolver }

    single { getRemoteConfig() }

    single { ConfigRepository(config = get(), seconds = 60L) as IConfigRepository }

    single { FirebaseFirestore.getInstance() }

    single { FirebaseStorage.getInstance() }

    single { RemoteUserRepository(store = get(), logger = get()) as IRemoteUserRepository }

    single { provideOkHttpClient(get()) }

    single(name = "speech") {
        provideRetrofit(okHttpClient = get(), url = BuildConfig.SPEECH_RECOGNITION_API_V1_ENDPOINT)
    }

    single {
        Moshi
            .Builder()
            .add(KotlinJsonAdapterFactory())
            .build() as Moshi
    }

    single { SimpleStorage(androidContext()) as ISimpleStorage }

    single {
        TokenRepository(
            simpleStorage = get(),
            configRepository = get(),
            moshi = get(),
            logger = get()
        ) as ITokenRepository
    }

    single {
        Recognizer(
            userRepository = get(),
            walletRepository = get(),
            client = get(),
            logger = get(),
            recordsRepository = get(),
            tokenRepository = get(),
            moshi = get(),
            configRepository = get()
        ) as IRecognizer
    }

    single { Recorder(androidContext()) as IRecorder }

    single { Player(get()) as IPlayer }

    single { WalletRepository() as IWalletRepository }

    single { RecordsRepository(db = get(), logger = get(), repo = get()) as IRecordsRepository }

    single {
        Synchronizer(
            firestore = get(),
            logger = get(),
            storage = get(),
            extension = FILE_EXTENSION,
            recordsRepository = get()
        ) as ISynchronizer
    }

    single {
        RecordsFetcher(
            db = get(),
            logger = get(),
            firestore = get()
        ) as IRecordsFetcher
    }

}

private fun getRemoteConfig(): FirebaseRemoteConfig {
    val config = FirebaseRemoteConfig.getInstance()
    val settings = FirebaseRemoteConfigSettings
        .Builder()
        .setDeveloperModeEnabled(true)
        .build()
    config.setConfigSettings(settings)
    val defValues = HashMap<String, Any>()
    //todo add defaults

    defValues[RAW_LANGUAGES] = "[]"
    defValues[SYNCHRONOUS_DURATION] = 15000L
    defValues[RAW_CREDENTIALS] = "{}"
    config.setDefaults(defValues)
    return config
}

private fun provideOkHttpClient(logger: ILogger): OkHttpClient {
    val okHttpClientBuilder = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(ResponseCodeInterceptor(logger))
    return okHttpClientBuilder.build()
}

private fun provideRetrofit(okHttpClient: OkHttpClient, url: String): Retrofit =
    Retrofit.Builder()
        .baseUrl(url)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
        .build()

private class ResponseCodeInterceptor(val logger: ILogger) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original: Request = chain.request()
        val url = String.format(
            "%s://%s%s",
            original.url().scheme(),
            original.url().host(),
            original.url().encodedPath()
        )
        val method = original.method()
        val response: Response
        val responseBody: ResponseBody?
        response = chain.proceed(original)
        responseBody = response.body()
        val code = response.code()
        val body = getBody(original.body())
        responseBody!!
        val rawJson: String = responseBody.string()//.add(code)
        logger
            .with(this)
            .add("$method $url ${body ?: ""} ")
            .add("Response is code=$code raw=$rawJson")
            .log()
        return response
            .newBuilder()
            .body(ResponseBody.create(responseBody.contentType(), rawJson))
            .code(200)
            .build()
    }

    fun String.add(code: Int): String {
        return StringBuilder(substringBeforeLast('}', "{$this"))
            .append(", \"response_code\":$code}")
            .toString()
    }

    companion object {
        const val ENCODING = "UTF-8"
        val UTF8: Charset = Charset.forName(ENCODING)
    }

    private fun getBody(requestBody: RequestBody?): String? {
        if (requestBody == null) return null
        val buffer = Buffer()
        try {
            requestBody.writeTo(buffer)
        } catch (e: IOException) {
            return null
        }

        if (!isPlaintext(buffer)) return null
        return java.net.URLDecoder.decode(buffer.readString(UTF8), ENCODING)
    }

    private fun isPlaintext(buffer: Buffer): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = if (buffer.size() < 64) buffer.size() else 64
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0..15) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            return true
        } catch (e: EOFException) {
            return false
        }
    }

}