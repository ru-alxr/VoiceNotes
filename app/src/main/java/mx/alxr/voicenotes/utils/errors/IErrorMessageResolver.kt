package mx.alxr.voicenotes.utils.errors

interface IErrorMessageResolver {

    fun resolve(throwable: Throwable): ErrorSolution
    fun resolve(throwable: Throwable, defaultInteraction: Interaction): ErrorSolution
    fun resolve(throwable: Throwable, defaultInteraction: Interaction, details:Any): ErrorSolution

}

const val REQUIRED_RECORD_LANGUAGE_CODE: String = "record_language_code_required"
const val REQUIRED_MORE_FUNDS: String = "more_funds_required"

class ErrorSolution(
    val message: String = "",
    val interaction: Interaction = Interaction.None,
    val resolutionRequired: String = "",
    val details:Any = Unit
)

enum class Interaction {
    None,
    Alert,
    Snack
}