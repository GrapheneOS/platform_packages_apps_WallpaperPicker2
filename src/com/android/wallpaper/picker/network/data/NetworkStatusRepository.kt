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

import kotlinx.coroutines.flow.Flow

/** An interface which allows consumers to collect network status information */
interface NetworkStatusRepository {

    /**
     * Returns a [Flow] that emits the current network connectivity status.
     *
     * The flow emits `true` when the network is available (connected) after being unavailable and
     * `false` otherwise
     *
     * The emitted values will update whenever the network status changes.
     *
     * @return A [Flow] of [Boolean] representing the network connectivity status.
     */
    fun networkStateFlow(): Flow<Boolean>
}
