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
import com.android.wallpaper.module.logging.UserEventLogger.Companion.DailyWallpaperMetadataFailureReason
import com.android.wallpaper.module.logging.UserEventLogger.Companion.DailyWallpaperUpdateCrash
import com.android.wallpaper.module.logging.UserEventLogger.Companion.DailyWallpaperUpdateResult
import com.android.wallpaper.module.logging.UserEventLogger.Companion.WallpaperSetFailureReason
import com.android.wallpaper.module.logging.UserEventLogger.Companion.WallpaperSetResult

/** [UserEventLogger] which does not do anything. */
open class NoOpUserEventLogger : UserEventLogger {
    override fun logAppLaunched(launchSource: Intent) {}
    override fun logActionClicked(collectionId: String, actionLabelResId: Int) {}
    override fun logIndividualWallpaperSelected(collectionId: String) {}
    override fun logCategorySelected(collectionId: String) {}
    override fun logSnapshot() {}
    override fun logWallpaperSet(collectionId: String?, wallpaperId: String?, effects: String?) {}
    override fun logWallpaperSetResult(@WallpaperSetResult result: Int) {}
    override fun logWallpaperSetFailureReason(@WallpaperSetFailureReason reason: Int) {}
    override fun logNumDailyWallpaperRotationsInLastWeek() {}
    override fun logNumDailyWallpaperRotationsPreviousDay() {}
    override fun logRefreshDailyWallpaperButtonClicked() {}
    override fun logDailyWallpaperRotationStatus(status: Int) {}
    override fun logDailyWallpaperSetNextWallpaperResult(@DailyWallpaperUpdateResult result: Int) {}

    override fun logDailyWallpaperSetNextWallpaperCrash(@DailyWallpaperUpdateCrash crash: Int) {}
    override fun logNumDaysDailyRotationFailed(days: Int) {}
    override fun logDailyWallpaperMetadataRequestFailure(
        @DailyWallpaperMetadataFailureReason reason: Int
    ) {}

    override fun logNumDaysDailyRotationNotAttempted(days: Int) {}
    override fun logStandalonePreviewLaunched() {}
    override fun logStandalonePreviewImageUriHasReadPermission(isReadPermissionGranted: Boolean) {}
    override fun logStandalonePreviewStorageDialogApproved(isApproved: Boolean) {}
    override fun logWallpaperPresentationMode() {}
    override fun logRestored() {}
    override fun logEffectApply(
        effect: String,
        status: Int,
        timeElapsedMillis: Long,
        resultCode: Int
    ) {}

    override fun logEffectProbe(effect: String, status: Int) {}
    override fun logEffectForegroundDownload(
        effect: String,
        status: Int,
        timeElapsedMillis: Long
    ) {}
}
