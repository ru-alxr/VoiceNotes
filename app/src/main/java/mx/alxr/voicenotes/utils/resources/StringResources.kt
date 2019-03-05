package mx.alxr.voicenotes.utils.resources

import android.content.Context

class StringResources(private val context:Context):IStringResources {

    override fun getString(resId: Int): String {
        return context.getString(resId)
    }

}