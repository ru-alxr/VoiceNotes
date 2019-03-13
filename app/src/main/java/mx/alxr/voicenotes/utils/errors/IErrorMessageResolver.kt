package mx.alxr.voicenotes.utils.errors

interface IErrorMessageResolver {

    fun resolve(throwable: Throwable): ErrorSolution
    fun resolve(throwable: Throwable, defaultInteraction: Interaction): ErrorSolution
    fun resolve(throwable: Throwable, defaultInteraction: Interaction, details:Map<String, String>): ErrorSolution

}

const val REQUIRED_RECORD_LANGUAGE_CODE: String = "record_language_code_required"
const val REQUIRED_USER_REGISTRATION: String = "user_registration_required"
const val REQUIRED_USER_NATIVE_LANGUAGE: String = "user_native_language"
const val REQUIRED_MORE_FUNDS: String = "more_funds_required"

class ErrorSolution(
    val message: String = "",
    val interaction: Interaction = Interaction.None,
    val resolutionRequired: String = "",
    val details:Map<String, String> = HashMap()
)

enum class Interaction {
    None,
    Alert,
    Snack
}