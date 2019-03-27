package mx.alxr.voicenotes.utils.logger

import android.util.Log

class AppLogger(private val enabled: Boolean) : ILogger, org.koin.log.Logger {

    override fun with(source: Any): ILogger.Builder {
        return if (enabled) {
            DebugBuilder(source)
        } else {
            getDummyBuilder()
        }
    }

    companion object DummyBuilderProvider {

        private val builder = DummyBuilder()

        fun getDummyBuilder(): ILogger.Builder {
            return builder
        }
    }

    class DummyBuilder : ILogger.Builder {
        override fun add(key: String, value: Any?): ILogger.Builder {
            return this
        }

        override fun log() {

        }

        override fun n(): ILogger.Builder {
            return this
        }

        override fun add(value: String): ILogger.Builder {
            return this
        }

        override fun e(throwable: Throwable) {

        }
    }

    class DebugBuilder(source: Any) : ILogger.Builder {
        override fun add(key: String, value: Any?): ILogger.Builder {
            stringBuilder.append(" {$key : $value}")
            return this
        }

        private val stringBuilder: StringBuilder = StringBuilder().append("From ")
            .append(if (source is String) source else source.javaClass.simpleName.toString()).append(": ")

        override fun add(value: String): ILogger.Builder {
            stringBuilder.append(value)
            stringBuilder.append("; ")
            return this
        }

        override fun n(): ILogger.Builder {
            stringBuilder.append("\n")
            return this
        }

        override fun log() {
            Log.d(
                "DEBUG_LOG",
                stringBuilder.toString()
            )
        }

        override fun e(throwable: Throwable) {
            stringBuilder.append(throwable.localizedMessage)
            stringBuilder.append("; ")
            Log.d(
                "DEBUG_LOG",
                stringBuilder.toString()
            )
            throwable.printStackTrace()
        }
    }

    override fun debug(msg: String) {
        with("Koin").add("Debug", msg).log()
    }

    override fun err(msg: String) {
        with("Koin").add("Error", msg).log()
    }

    override fun info(msg: String) {
        with("Koin").add("Info", msg).log()
    }

}