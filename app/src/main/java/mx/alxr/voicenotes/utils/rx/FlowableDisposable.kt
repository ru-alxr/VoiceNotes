package mx.alxr.voicenotes.utils.rx

import io.reactivex.subscribers.DisposableSubscriber

class FlowableDisposable<T>(
    private val next: (T) -> Unit,
    private val error: (Throwable) -> Unit = {},
    private val complete: () -> Unit = {}
) :
    DisposableSubscriber<T>() {

    override fun onComplete() {
        complete.invoke()
    }

    override fun onNext(t: T) {
        next.invoke(t)
    }

    override fun onError(t: Throwable) {
        error.invoke(t)
    }

}