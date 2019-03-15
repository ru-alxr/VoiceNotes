package mx.alxr.voicenotes.utils.errors

import com.google.firebase.firestore.FirebaseFirestoreException
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.utils.resources.IStringResources
import java.net.UnknownHostException

class ErrorMessageResolver(val resources: IStringResources) : IErrorMessageResolver {

    override fun resolve(throwable: Throwable): ErrorSolution {
        return ErrorSolution(message = resolveMessage(throwable), resolutionRequired = resolveRequiredData(throwable))
    }

    override fun resolve(throwable: Throwable, defaultInteraction: Interaction): ErrorSolution {
        return ErrorSolution(message = resolveMessage(throwable), interaction = defaultInteraction, resolutionRequired = resolveRequiredData(throwable))
    }

    override fun resolve(
        throwable: Throwable,
        defaultInteraction: Interaction,
        details: Map<String, String>
    ): ErrorSolution {
        return ErrorSolution(
            message = resolveMessage(throwable),
            interaction = defaultInteraction,
            resolutionRequired = resolveRequiredData(throwable),
            details = details
        )
    }

    private fun resolveMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> resources.getString(R.string.no_network_error)
            is java.net.SocketTimeoutException -> resources.getString(R.string.no_network_error)
            is ProjectException -> resources.getString(throwable.messageId)

            is FirebaseFirestoreException -> resources.getString(R.string.no_network_error)
            else -> resources.getString(R.string.something_went_wrong)
        }
    }

    private fun resolveRequiredData(throwable: Throwable): String{
        return if (throwable is ProjectException){
            when(throwable.messageId){
                R.string.error_no_funds -> REQUIRED_MORE_FUNDS
                R.string.error_record_language_required -> REQUIRED_RECORD_LANGUAGE_CODE
                R.string.error_registration_required -> REQUIRED_USER_REGISTRATION
                else -> ""
            }
        }else{
            ""
        }
    }

}