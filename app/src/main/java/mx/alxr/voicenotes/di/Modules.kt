package mx.alxr.voicenotes.di

import mx.alxr.voicenotes.MainViewModel
import mx.alxr.voicenotes.feature.auth.AuthViewModel
import mx.alxr.voicenotes.feature.init.InitViewModel
import mx.alxr.voicenotes.feature.preload.PreloadViewModel
import mx.alxr.voicenotes.feature.restore.RestoreViewModel
import mx.alxr.voicenotes.feature.selector.LanguageSelectorViewModel
import mx.alxr.voicenotes.feature.working.WorkingViewModel
import mx.alxr.voicenotes.feature.working.home.HomeViewModel
import mx.alxr.voicenotes.feature.working.records.RecordsViewModel
import mx.alxr.voicenotes.feature.working.settings.SettingsViewModel
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val FEATURE_INIT_MODULE = module {

    viewModel { InitViewModel() }

}

val FEATURE_AUTH = module {

    viewModel {
        AuthViewModel(
            navigation = get(),
            mLogger = get(),
            errorResolver = get(),
            userRepository = get(),
            remoteUserRepository = get(),
            wallet = get()
        )
    }

}

val FEATURE_FETCH_RECORDS = module {
    viewModel {
        RestoreViewModel(
            logger = get(),
            navigation = get(),
            userRepository = get(),
            errorResolver = get(),
            fetcher = get()
        )
    }
}

val FEATURE_PRELOAD_MODULE = module {

    viewModel {
        PreloadViewModel(
            languageRepository = get(),
            errorResolver = get(),
            navigation = get(),
            logger = get()
        )
    }

}

val FEATURE_LANGUAGE_SELECTOR = module {

    viewModel {
        LanguageSelectorViewModel(
            db = get(),
            nav = get(), remoteUserRepository = get(),
            userRepository = get(),
            errorMessageResolver = get(),
            recordsRepository = get(),
            logger = get()

        )
    }

}

val MAIN_VIEW_MODULE = module {

    viewModel { MainViewModel(featureNavigation = get(), userRepository = get()) }

    viewModel { HomeViewModel(userRepository = get(), logger = get(), recorder = get(), synchronizer = get()) }

    viewModel { SettingsViewModel(userRepository = get(), nav = get()) }

    viewModel { WorkingViewModel(get()) }

    viewModel {
        RecordsViewModel(
            db = get(),
            player = get(),
            synchronizer = get(),
            resolver = get(),
            logger = get(),
            recognizer = get(),
            navigation = get(),
            map = HashMap(),
            recordsRepository = get(),
            promoter = get()
        )
    }

}