package com.neddy.ketch

import android.app.Application
import android.content.Context
import com.neddy.ketch.di.AppContainer

class KetchApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.createChannels()
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as KetchApp).container
