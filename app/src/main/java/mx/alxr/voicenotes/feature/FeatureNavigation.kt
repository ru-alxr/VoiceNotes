package mx.alxr.voicenotes.feature

import mx.alxr.voicenotes.utils.logger.ILogger

class FeatureNavigation(private val logger:ILogger): IFeatureNavigation {

    private var mHandler: IHandler? = null

    override fun detach() {
        logger.with(this).add("detach $mHandler").log()
        mHandler = null
    }

    override fun navigateFeature(target: Int) {
        logger.with(this).add("navigateFeature $target $mHandler").log()
        mHandler?.onFeatureRequested(target)
    }

    override fun attach(handler: IHandler) {
        logger.with(this).add("detach $handler").log()
        mHandler = handler
    }

    override fun navigateFeature(target: Int, args: Any) {
        logger.with(this).add("navigateFeature $target $mHandler").log()
        mHandler?.onFeatureRequested(target, args)
    }

}