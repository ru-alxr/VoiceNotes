package mx.alxr.voicenotes.feature

const val FEATURE_INIT: Int = 1
const val FEATURE_PRELOAD: Int = 2
const val FEATURE_SELECT_NATIVE_LANGUAGE = 3
const val FEATURE_WORKING: Int = 4

interface IFeatureNavigation {

    fun navigateFeature(target: Int)

    fun navigateFeature(target: Int, args:Any)

    fun attach(handler: IHandler)

    fun detach()

}

interface IHandler {

    fun onFeatureRequested(target: Int)

    fun onFeatureRequested(target: Int, args:Any)

}