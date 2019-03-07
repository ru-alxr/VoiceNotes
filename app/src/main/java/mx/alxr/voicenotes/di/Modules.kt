package mx.alxr.voicenotes.di

import mx.alxr.voicenotes.MainViewModel
import mx.alxr.voicenotes.feature.init.InitViewModel
import mx.alxr.voicenotes.feature.preload.PreloadViewModel
import mx.alxr.voicenotes.feature.selector.LanguageSelectorViewModel
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val FEATURE_INIT_MODULE = module {

    viewModel { InitViewModel() }

}

val FEATURE_PRELOAD_MODULE = module {

    viewModel { PreloadViewModel(languageRepository = get(), errorResolver = get(), navigation = get(), logger = get()) }

}

val FEATURE_LANGUAGE_SELECTOR = module{

    viewModel { LanguageSelectorViewModel(db = get()) }

}

val MAIN_VIEW_MODULE = module {

    viewModel { MainViewModel(featureNavigation = get(), userRepository = get()) }

}