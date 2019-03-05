package mx.alxr.voicenotes.feature

class FeatureNavigation: IFeatureNavigation {

    private var mHandler: IHandler? = null

    override fun detach() {
        mHandler = null
    }

    override fun navigateFeature(target: Int) {
        mHandler?.onFeatureRequested(target)
    }

    override fun attach(handler: IHandler) {
        mHandler = handler
    }

    override fun navigateFeature(target: Int, args: Any) {
        mHandler?.onFeatureRequested(target, args)
    }

}