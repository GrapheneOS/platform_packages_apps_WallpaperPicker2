/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.picker.network.data

import android.content.Context
import android.util.Log
import com.android.wallpaper.module.NetworkStatusNotifier
import com.android.wallpaper.module.NetworkStatusNotifier.NETWORK_CONNECTED
import com.android.wallpaper.module.NetworkStatusNotifier.NETWORK_NOT_INITIALIZED
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
open class DefaultNetworkStatusRepository
@Inject
constructor(
    @ApplicationContext val context: Context,
    private val networkStatusNotifier: NetworkStatusNotifier,
) : NetworkStatusRepository {

    private val _networkStatus = MutableStateFlow<Int>(NETWORK_NOT_INITIALIZED)

    init {
        _networkStatus.value = networkStatusNotifier.networkStatus
    }

    override fun networkStateFlow(): Flow<Boolean> = callbackFlow {
        val listener =
            NetworkStatusNotifier.Listener { status: Int ->
                Log.i(DefaultNetworkStatusRepository.TAG, "Network status changes: " + status)
                if (_networkStatus.value != NETWORK_CONNECTED && status == NETWORK_CONNECTED) {
                    // Emit true value when network is available and it was previously unavailable
                    trySend(true)
                } else {
                    trySend(false)
                }

                _networkStatus.value = networkStatusNotifier.networkStatus
            }

        // Register the listener with the network status notifier
        networkStatusNotifier.registerListener(listener)

        // Await close and unregister listener to avoid memory leaks
        awaitClose { networkStatusNotifier.unregisterListener(listener) }
    }

    companion object {
        private const val TAG = "DefaultNetworkStatusRepository"
    }
}
