package mx.alxr.voicenotes.feature

const val FEATURE_INIT: Int = 1
const val FEATURE_PRELOAD: Int = 2
const val FEATURE_SELECT_NATIVE_LANGUAGE = 3
const val FEATURE_WORKING: Int = 4
const val FEATURE_AUTH = 5
const val FEATURE_LOAD_RECORDS = 6

const val FEATURE_BACK: Int = 7

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