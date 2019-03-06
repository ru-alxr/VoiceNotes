package mx.alxr.voicenotes.repository.gca

/**
 * Extracts key from assets
 * As long as key is restricted, this approach aimed to hide key from public code repo only
 */
interface IGoogleCloudApiKeyRepository {

    fun getSpeechToTextApiKey():String

    fun getFirebaseFirestoreApiKey():String

}