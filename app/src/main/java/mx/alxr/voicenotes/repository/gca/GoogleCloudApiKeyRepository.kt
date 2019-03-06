package mx.alxr.voicenotes.repository.gca

import android.content.Context
import org.json.JSONObject

class GoogleCloudApiKeyRepository(context: Context) : IGoogleCloudApiKeyRepository {

    private data class Data(val key1: String, val key2: String)

    private val keys: Data = initKeys(context)

    override fun getFirebaseFirestoreApiKey(): String {
        return keys.key2
    }

    override fun getSpeechToTextApiKey(): String {
        return keys.key1
    }

    private fun initKeys(context: Context): Data {
        val source = context
            .assets
            .open("api_key.txt")
            .bufferedReader()
            .use { it.readText() }
        val json = JSONObject(source)
        return Data(
            json.getString("speech_to_text_api"),
            json.getString("firebase_firestore_api")
        )
    }

}