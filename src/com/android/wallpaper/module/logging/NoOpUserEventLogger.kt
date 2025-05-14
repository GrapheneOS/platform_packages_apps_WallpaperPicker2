/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.wallpaper.module.logging

import android.content.Intent
import android.stats.style.StyleEnums
import com.android.wallpaper.module.logging.UserEventLogger.CustomizationPickerScreen
import com.android.wallpaper.module.logging.UserEventLogger.SetWallpaperEntryPoint
import com.android.wallpaper.module.logging.UserEventLogger.WallpaperDestination
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import io.grpc.Status

/** [UserEventLogger] implementation that does nothing. */
open class NoOpUserEventLogger : UserEventLogger {

    override suspend fun logSnapshot() {}

    override fun logAppLaunched(launchSource: Intent) {}

    override fun logWallpaperApplied(
        collectionId: String?,
        wallpaperId: String?,
        effects: String?,
        @SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
        @WallpaperDestination destination: Int,
    ) {}

    override fun logEffectApply(
        effect: String,
        status: Int,
        timeElapsedMillis: Long,
        resultCode: Int,
    ) {}

    override fun logEffectProbe(effect: String, status: Int) {}

    override fun logEffectForegroundDownload(
        effect: String,
        status: Int,
        timeElapsedMillis: Long,
    ) {}

    override fun logResetApplied() {}

    override fun logWallpaperExploreButtonClicked() {}

    override fun logEnterScreen(screen: Int) {}

    @CustomizationPickerScreen
    override fun transformCustomizationOptionToScreenForLogging(
        customizationOption: CustomizationOption
    ): Int {
        return StyleEnums.SCREEN_UNSPECIFIED
    }

    override fun logCuratedPhotosRendered(timeElapsedMillis: Long, userPhoto: Boolean) {}

    override fun logCuratedPhotosFetched(timeElapsedMillis: Long, status: Status) {}
}
