package com.neddy.ketch.di

import android.content.Context
import com.neddy.ketch.data.WatcherRepository
import com.neddy.ketch.data.local.KetchDatabase
import com.neddy.ketch.data.location.LocationProvider
import com.neddy.ketch.data.settings.SettingsRepository
import com.neddy.ketch.data.transit.TransitRepository
import com.neddy.ketch.data.transit.google.GoogleTransitRepository
import com.neddy.ketch.data.transit.google.PlacesApiService
import com.neddy.ketch.data.transit.google.RoutesApiService
import com.neddy.ketch.trigger.GeofenceManager
import com.neddy.ketch.trigger.NotificationHelper
import com.neddy.ketch.trigger.TriggerSyncRequester
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Simple manual dependency container. The app is small enough that a full
 * dependency injection framework is not worth its build cost.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val okHttpClient: OkHttpClient by lazy { OkHttpClient() }

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val watcherRepository: WatcherRepository by lazy {
        WatcherRepository(KetchDatabase.get(appContext).watcherDao())
    }

    val transitRepository: TransitRepository by lazy {
        GoogleTransitRepository(
            routesApi = retrofit("https://routes.googleapis.com/")
                .create(RoutesApiService::class.java),
            placesApi = retrofit("https://places.googleapis.com/")
                .create(PlacesApiService::class.java),
            settingsRepository = settingsRepository,
        )
    }

    val locationProvider: LocationProvider by lazy { LocationProvider(appContext) }

    val notificationHelper: NotificationHelper by lazy { NotificationHelper(appContext) }

    val geofenceManager: GeofenceManager by lazy { GeofenceManager(appContext) }

    val triggerSyncRequester: TriggerSyncRequester by lazy { TriggerSyncRequester(appContext) }
}
