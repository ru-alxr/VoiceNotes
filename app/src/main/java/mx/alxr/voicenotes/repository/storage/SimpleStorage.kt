package mx.alxr.voicenotes.repository.storage

import android.content.Context
import android.preference.PreferenceManager

class SimpleStorage(private val context: Context) : ISimpleStorage {

    override fun put(key: String, value: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs
            .edit()
            .putString(key, value)
            .apply()
    }

    override fun get(key: String, defValue: String?): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(key, defValue)
    }

}