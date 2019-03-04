package mx.alxr.voicenotes.utils.logger

interface ILogger {

    fun with(source: Any): Builder

    interface Builder {

        fun add(value: String): Builder

        fun add(key: String, value: Any?): Builder

        fun log()

    }

}