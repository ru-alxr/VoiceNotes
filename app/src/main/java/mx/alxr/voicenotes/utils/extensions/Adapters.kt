package mx.alxr.voicenotes.utils.extensions

import androidx.recyclerview.widget.RecyclerView

fun <T : RecyclerView.ViewHolder> RecyclerView.Adapter<T>.getPosition(matcher:(Int)->Boolean): Int {
    for (index in 0 until itemCount) {
        if (matcher.invoke(index)) return index
    }
    return -1
}