package mx.alxr.voicenotes.di

import mx.alxr.voicenotes.MainViewModel
import mx.alxr.voicenotes.feature.auth.AuthViewModel
import mx.alxr.voicenotes.feature.init.InitViewModel
import mx.alxr.voicenotes.feature.player.IPlayer
import mx.alxr.voicenotes.feature.player.Player
import mx.alxr.voicenotes.feature.preload.PreloadViewModel
import mx.alxr.voicenotes.feature.recognizer.IRecognizer
import mx.alxr.voicenotes.feature.recognizer.Recognizer
import mx.alxr.voicenotes.feature.recorder.FILE_EXTENSION
import mx.alxr.voicenotes.feature.recorder.IRecorder
import mx.alxr.voicenotes.feature.recorder.Recorder
import mx.alxr.voicenotes.feature.selector.LanguageSelectorViewModel
import mx.alxr.voicenotes.feature.synchronizer.ISynchronizer
import mx.alxr.voicenotes.feature.synchronizer.Synchronizer
import mx.alxr.voicenotes.feature.working.WorkingViewModel
import mx.alxr.voicenotes.feature.working.home.HomeViewModel
import mx.alxr.voicenotes.feature.working.records.RecordsViewModel
import mx.alxr.voicenotes.feature.working.settings.SettingsViewModel
import mx.alxr.voicenotes.repository.media.IMediaStorage
import mx.alxr.voicenotes.repository.media.MediaStorage
import mx.alxr.voicenotes.repository.record.IRecordsRepository
import mx.alxr.voicenotes.repository.record.RecordsRepository
import mx.alxr.voicenotes.repository.wallet.IWalletRepository
import mx.alxr.voicenotes.repository.wallet.WalletRepository
import org.koin.android.ext.koin.androidContext
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
            remoteUserRepository = get()
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

    viewModel { HomeViewModel(userRepository = get(), logger = get(), recorder = get(), storage = get()) }

    viewModel { SettingsViewModel(userRepository = get(), nav = get()) }

    viewModel { WorkingViewModel(get()) }

    viewModel {
        RecordsViewModel(
            db = get(),
            player = get(),
            storage = get(),
            resolver = get(),
            logger = get(),
            recognizer = get(),
            navigation = get()
        )
    }

    single { Recorder(androidContext()) as IRecorder }

    single { MediaStorage(recordsRepository = get(), extension = FILE_EXTENSION) as IMediaStorage }

    single { RecordsRepository(db = get(), logger = get(), repo = get()) as IRecordsRepository }

    single { Player(get()) as IPlayer }

    single { Recognizer(mediaStorage = get(), userRepository = get(), walletRepository = get()) as IRecognizer }

    single { WalletRepository() as IWalletRepository }

    single { Synchronizer(db = get(), store = get(), logger = get()) as ISynchronizer }

}