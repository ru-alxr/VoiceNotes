package mx.alxr.voicenotes.feature.auth

data class ExtractedFirebaseUser(
    val email: String?,
    val uid: String,
    val displayName: String?,
    val providerId: String
)