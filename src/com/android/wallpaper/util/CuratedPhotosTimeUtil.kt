/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CuratedPhotosTimeUtil @Inject constructor() {

    private var startTime = 0L

    /** This method returns the current time in case start time is not set to 0. */
    fun getStartTime(): Long {
        return if (startTime != 0L) startTime else System.currentTimeMillis()
    }

    /** This method sets the start time when fetching of curated photos starts. */
    fun setStartTime(fetchStartTime: Long) {
        this.startTime = fetchStartTime
    }
}
