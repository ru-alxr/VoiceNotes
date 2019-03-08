package mx.alxr.voicenotes.utils.extensions

import androidx.fragment.app.Fragment

fun Fragment.format(resId:Int, arg: String):String{
    return String.format(getString(resId), arg)
}