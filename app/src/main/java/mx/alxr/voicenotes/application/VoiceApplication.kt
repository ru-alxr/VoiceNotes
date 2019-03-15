package mx.alxr.voicenotes.application

import android.app.Application
import android.content.Context
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.di.*
import mx.alxr.voicenotes.utils.logger.AppLogger
import mx.alxr.voicenotes.utils.logger.ILogger
import org.koin.android.ext.android.startKoin

class VoiceApplication : Application() {

    private lateinit var mDebugLogger: ILogger

    override fun onCreate() {
        super.onCreate()
        startKoin(
            this,
            listOf(
                APPLICATION_MODULE,
                FEATURE_AUTH,
                FEATURE_INIT_MODULE,
                FEATURE_PRELOAD_MODULE,
                FEATURE_LANGUAGE_SELECTOR,
                MAIN_VIEW_MODULE
            )
        )
        addUndeliverableErrorHandler()
    }

    private fun addUndeliverableErrorHandler() {
        RxJavaPlugins.setErrorHandler { e ->
            mDebugLogger.with(this).add("Error $e").log()
            if (e is UndeliverableException) return@setErrorHandler
            throw e as Exception
        }
    }

    fun getLogger(): ILogger {
        if (!::mDebugLogger.isInitialized) mDebugLogger = AppLogger(resources.getBoolean(R.bool.enableDebugLogging))
        return mDebugLogger
    }

}

fun Context.getAppLogger(): ILogger {
    return (applicationContext as VoiceApplication).getLogger()
}