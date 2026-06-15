package com.example.animepopular

import android.app.Application
import com.example.animepopular.di.AppContainer
import timber.log.Timber

class AnimeApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("AnimeApplication started")
        }
    }
}