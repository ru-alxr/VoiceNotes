package mx.alxr.voicenotes.di

import android.view.LayoutInflater
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import mx.alxr.voicenotes.application.getAppLogger
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.feature.FeatureNavigation
import mx.alxr.voicenotes.feature.IFeatureNavigation
import mx.alxr.voicenotes.repository.config.ConfigRepository
import mx.alxr.voicenotes.repository.config.IConfigRepository
import mx.alxr.voicenotes.repository.config.RAW_LANGUAGES
import mx.alxr.voicenotes.repository.gca.GoogleCloudApiKeyRepository
import mx.alxr.voicenotes.repository.gca.IGoogleCloudApiKeyRepository
import mx.alxr.voicenotes.repository.language.ILanguageRepository
import mx.alxr.voicenotes.repository.language.LanguageRepository
import mx.alxr.voicenotes.repository.remote.firebaseuser.IRemoteUserRepository
import mx.alxr.voicenotes.repository.remote.firebaseuser.RemoteUserRepository
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.repository.user.UserRepository
import mx.alxr.voicenotes.utils.errors.ErrorMessageResolver
import mx.alxr.voicenotes.utils.errors.IErrorMessageResolver
import mx.alxr.voicenotes.utils.resources.IStringResources
import mx.alxr.voicenotes.utils.resources.StringResources
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val APPLICATION_MODULE = module {

    single { androidContext().getAppLogger() }

    single { LayoutInflater.from(androidContext()) }

    single { UserRepository(db = get(), logger = get()) as IUserRepository }

    single(createOnStart = true) { AppDatabase.getInstance(androidContext()) as AppDatabase }

    single { FeatureNavigation() as IFeatureNavigation }

    single { StringResources(androidContext()) as IStringResources }

    single { LanguageRepository(configRepo = get(), db = get(), logger = get()) as ILanguageRepository }

    single { ErrorMessageResolver(resources = get()) as IErrorMessageResolver }

    single { GoogleCloudApiKeyRepository(androidContext()) as IGoogleCloudApiKeyRepository }

    single { getRemoteConfig() }

    single { ConfigRepository(config = get(), seconds = 60L) as IConfigRepository }

    single { FirebaseFirestore.getInstance() }

    single { RemoteUserRepository(store = get(), logger = get()) as IRemoteUserRepository }

}

private fun getRemoteConfig(): FirebaseRemoteConfig {
    val config = FirebaseRemoteConfig.getInstance()
    val settings = FirebaseRemoteConfigSettings
        .Builder()
        .setDeveloperModeEnabled(true)
        .build()
    config.setConfigSettings(settings)
    val defValues = HashMap<String, Any>()
    //todo add defaults

    defValues[RAW_LANGUAGES] = "[]"
    config.setDefaults(defValues)
    return config
}