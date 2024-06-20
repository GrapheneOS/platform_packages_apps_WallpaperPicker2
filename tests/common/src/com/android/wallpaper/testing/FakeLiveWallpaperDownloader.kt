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

package com.android.wallpaper.testing

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.data.util.LiveWallpaperDownloader
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeLiveWallpaperDownloader @Inject constructor() : LiveWallpaperDownloader {

    private var liveWallpaperDownloadListener:
        LiveWallpaperDownloader.LiveWallpaperDownloadListener? =
        null

    fun proceedToDownloadSuccess(result: WallpaperModel.LiveWallpaperModel) {
        liveWallpaperDownloadListener?.onDownloadSuccess(result)
    }

    fun proceedToDownloadFailed() {
        liveWallpaperDownloadListener?.onDownloadFailed()
    }

    private val _isDownloaderReady = MutableStateFlow(false)
    override val isDownloaderReady: Flow<Boolean> = _isDownloaderReady.asStateFlow()

    /**
     * This is to simulate [initiateDownloadableService] without passing [Activity], for testing
     * purpose.
     */
    fun initiateDownloadableServiceByPass() {
        _isDownloaderReady.value = true
    }

    override fun initiateDownloadableService(
        activity: Activity,
        wallpaperData: WallpaperModel.StaticWallpaperModel,
        intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        _isDownloaderReady.value = true
    }

    override fun cleanup() {}

    override fun downloadWallpaper(
        listener: LiveWallpaperDownloader.LiveWallpaperDownloadListener
    ) {
        liveWallpaperDownloadListener = listener
        // Please call proceedToDownloadSuccess() and proceedToDownloadFailed() in the test to
        // simulate download resolutions.
    }

    var isCancelDownloadWallpaperCalled = false

    override fun cancelDownloadWallpaper(): Boolean {
        isCancelDownloadWallpaperCalled = true
        return false
    }
}
