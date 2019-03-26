package mx.alxr.voicenotes.utils.errors

class ProjectException(val messageId: Int, val args:Any? = null) : Exception()