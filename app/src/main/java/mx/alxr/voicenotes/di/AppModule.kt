package mx.alxr.voicenotes.di

import mx.alxr.voicenotes.application.getAppLogger
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.feature.FeatureNavigation
import mx.alxr.voicenotes.feature.IFeatureNavigation
import mx.alxr.voicenotes.repository.language.ILanguageRepository
import mx.alxr.voicenotes.repository.language.LanguageRepository
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

    single { UserRepository(db = get()) as IUserRepository }

    single(createOnStart = true) { AppDatabase.getInstance(androidContext()) as AppDatabase }

    single { FeatureNavigation() as IFeatureNavigation }

    single { StringResources(androidContext()) as IStringResources }

    single { LanguageRepository() as ILanguageRepository }

    single { ErrorMessageResolver(resources = get()) as IErrorMessageResolver }


}