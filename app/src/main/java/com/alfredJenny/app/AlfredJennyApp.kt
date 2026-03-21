package com.alfredJenny.app

import android.app.Application
import com.alfredJenny.app.data.repository.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AlfredJennyApp : Application() {

    /**
     * Injecting AuthRepository here ensures it is instantiated at app start,
     * which triggers its init block to restore the JWT + baseUrl from DataStore
     * into [TokenStore] before any screen tries to make an API call.
     */
    @Inject lateinit var authRepository: AuthRepository
}
