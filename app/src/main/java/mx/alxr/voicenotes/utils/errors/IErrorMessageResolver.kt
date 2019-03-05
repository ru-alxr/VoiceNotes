package mx.alxr.voicenotes.utils.errors

interface IErrorMessageResolver {

    fun resolve(throwable: Throwable):ErrorSolution
    fun resolve(throwable: Throwable, defaultInteraction: Interaction):ErrorSolution

}

class ErrorSolution(val message:String = "", val interaction: Interaction = Interaction.None)

enum class Interaction{
    None,
    Alert,
    Snack
}