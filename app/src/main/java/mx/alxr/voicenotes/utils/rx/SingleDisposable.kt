package mx.alxr.voicenotes.utils.rx

import io.reactivex.observers.DisposableSingleObserver

class SingleDisposable<T>(private val success: (T) -> Unit, private val error: (Throwable) -> Unit = {}) : DisposableSingleObserver<T>() {
    override fun onSuccess(t: T) {
        success.invoke(t)
    }

    override fun onError(e: Throwable) {
        e.printStackTrace()
        error.invoke(e)
    }
}