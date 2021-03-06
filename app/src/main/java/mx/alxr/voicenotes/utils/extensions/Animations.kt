package mx.alxr.voicenotes.utils.extensions

import android.view.animation.Animation

fun Animation.onAnimationEnd(callback: () -> Unit): Animation {
    setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {

        }

        override fun onAnimationEnd(animation: Animation?) {
            callback.invoke()
        }

        override fun onAnimationStart(animation: Animation?) {

        }
    })
    return this
}

fun Animation.setCustomDuration(duration: Long): Animation {
    setDuration(duration)
    return this
}

fun Animation.setCustomFillAfter(value: Boolean): Animation {
    fillAfter = value
    return this
}