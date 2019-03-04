package mx.alxr.voicenotes.di

import mx.alxr.voicenotes.application.getAppLogger
import mx.alxr.voicenotes.db.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module.module

val APPLICATION_MODULE = module {

    single { androidContext().getAppLogger() }

    single(createOnStart = true) { AppDatabase.getInstance(androidContext()) as AppDatabase }

//    single(createOnStart = true) { AppDatabase.getInstance(androidContext()) as AppDatabase }
//    single { UsersRepository(db = get(), logger = get(), retrofit = get()) as IUsersRepository }
//    single { MobsRepository(get(), get(), get()) as IMobsRepository }
//    single { FeatureNavigation() as IFeatureNavigation }
//
//    single { LayoutInflater.from(androidContext()) as LayoutInflater }
//
//    single { RecordsRepository(db = get(), retrofit = get()) as IRecordsRepository }
//    viewModel { MainViewModel(get(), get()) }

}
