package mx.alxr.voicenotes.application

import android.app.Application
import android.content.Context
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.di.APPLICATION_MODULE
import mx.alxr.voicenotes.di.FEATURE_INIT_MODULE
import mx.alxr.voicenotes.di.FEATURE_PRELOAD_MODULE
import mx.alxr.voicenotes.di.MAIN_VIEW_MODULE
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
                FEATURE_INIT_MODULE,
                FEATURE_PRELOAD_MODULE,
                MAIN_VIEW_MODULE
            )
        )
        addUndeliverableErrorHandler()
    }

    private fun addUndeliverableErrorHandler() {
        RxJavaPlugins.setErrorHandler { e ->
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