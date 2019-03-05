package mx.alxr.voicenotes.utils.errors

import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.resources.IStringResources
import java.net.UnknownHostException

class ErrorMessageResolver(val resources: IStringResources) : IErrorMessageResolver {

    override fun resolve(throwable: Throwable): ErrorSolution {
        return ErrorSolution(message = resolveMessage(throwable))
    }

    override fun resolve(throwable: Throwable, defaultInteraction: Interaction): ErrorSolution {
        return ErrorSolution(message = resolveMessage(throwable), interaction = defaultInteraction)
    }

    private fun resolveMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> resources.getString(R.string.no_network_error)
            is java.net.SocketTimeoutException -> resources.getString(R.string.no_network_error)
            is ProjectException -> resources.getString(throwable.messageId)

            else -> resources.getString(R.string.something_went_wrong)
        }
    }

}