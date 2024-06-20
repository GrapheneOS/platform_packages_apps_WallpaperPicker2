/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.wallpaper.picker.preview.data.util

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import kotlinx.coroutines.flow.Flow

/**
 * Handles the download process of a downloadable wallpaper. This downloader should be aware of the
 * Activity's lifecycle.
 */
interface LiveWallpaperDownloader {

    val isDownloaderReady: Flow<Boolean>

    interface LiveWallpaperDownloadListener {
        fun onDownloadSuccess(wallpaperModel: LiveWallpaperModel)

        fun onDownloadFailed()
    }

    /**
     * Initializes the downloadable service. This needs to be called when [Activity.onCreate] and
     * before calling [downloadWallpaper].
     */
    fun initiateDownloadableService(
        activity: Activity,
        wallpaperData: StaticWallpaperModel,
        intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>,
    )

    /**
     * Clean up the underlying downloadable service. This needs to be called when
     * [Activity.onDestroy].
     */
    fun cleanup()

    fun downloadWallpaper(listener: LiveWallpaperDownloadListener)

    /** @return True if there is a confirm cancel download dialog from the download service. */
    fun cancelDownloadWallpaper(): Boolean
}
