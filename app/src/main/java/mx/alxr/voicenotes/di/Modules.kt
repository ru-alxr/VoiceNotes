package mx.alxr.voicenotes.di

import mx.alxr.voicenotes.feature.init.InitViewModel
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val FEATURE_INIT_MODULE = module {

    viewModel { InitViewModel() }

}