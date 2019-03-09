package mx.alxr.voicenotes.di

import mx.alxr.voicenotes.MainViewModel
import mx.alxr.voicenotes.feature.working.home.HomeViewModel
import mx.alxr.voicenotes.feature.init.InitViewModel
import mx.alxr.voicenotes.feature.preload.PreloadViewModel
import mx.alxr.voicenotes.feature.recorder.IRecorder
import mx.alxr.voicenotes.feature.recorder.Recorder
import mx.alxr.voicenotes.feature.selector.LanguageSelectorViewModel
import mx.alxr.voicenotes.feature.working.WorkingViewModel
import mx.alxr.voicenotes.feature.working.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val FEATURE_INIT_MODULE = module {

    viewModel { InitViewModel() }

}

val FEATURE_PRELOAD_MODULE = module {

    viewModel { PreloadViewModel(languageRepository = get(), errorResolver = get(), navigation = get(), logger = get()) }

}

val FEATURE_LANGUAGE_SELECTOR = module{

    viewModel { LanguageSelectorViewModel(db = get(), nav = get(), userRepository = get(), logger = get()) }

}

val MAIN_VIEW_MODULE = module {

    viewModel { MainViewModel(featureNavigation = get(), userRepository = get()) }

    viewModel { HomeViewModel(userRepository = get(), logger = get()) }

    viewModel { SettingsViewModel(userRepository = get(), nav = get()) }

    viewModel { WorkingViewModel(get()) }

    single { Recorder(androidContext()) as IRecorder }

}