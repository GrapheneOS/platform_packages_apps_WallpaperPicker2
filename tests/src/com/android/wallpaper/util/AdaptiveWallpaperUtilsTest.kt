/*
 * Copyright (C) 2022 The Android Open Source Project
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

import com.android.wallpaper.model.AdaptiveType
import org.junit.Test
import java.util.*

/**
 * Test implementation of [AdaptiveWallpaperUtils].
 */
class AdaptiveWallpaperUtilsTest {

    @Test
    fun getAdaptiveWallpaperFilename_basedOnAdaptiveType() {
        assert(
            AdaptiveWallpaperUtils.getAdaptiveWallpaperFilename(AdaptiveType.DARK)
                    == "wp-adaptive-dark.png"
        )
        assert(
            AdaptiveWallpaperUtils.getAdaptiveWallpaperFilename(AdaptiveType.LIGHT)
                    == "wp-adaptive-light.png"
        )
    }

    @Test
    fun getCurrentAdaptiveType_basedTime() {
        val calendar = Calendar.getInstance()
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        calendar[Calendar.HOUR_OF_DAY] = 3
        assert(
            AdaptiveWallpaperUtils.getCurrentAdaptiveType(calendar.timeInMillis, null)
                    == AdaptiveType.DARK
        )
        calendar[Calendar.HOUR_OF_DAY] = 9
        assert(
            AdaptiveWallpaperUtils.getCurrentAdaptiveType(calendar.timeInMillis, null)
                    == AdaptiveType.LIGHT
        )
        calendar[Calendar.HOUR_OF_DAY] = 15
        assert(
            AdaptiveWallpaperUtils.getCurrentAdaptiveType(calendar.timeInMillis, null)
                    == AdaptiveType.LIGHT
        )
        calendar[Calendar.HOUR_OF_DAY] = 21
        assert(
            AdaptiveWallpaperUtils.getCurrentAdaptiveType(calendar.timeInMillis, null)
                    == AdaptiveType.DARK
        )
    }
}