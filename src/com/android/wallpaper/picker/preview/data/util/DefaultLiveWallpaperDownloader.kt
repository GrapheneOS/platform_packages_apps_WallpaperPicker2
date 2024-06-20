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
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.data.util.LiveWallpaperDownloader.LiveWallpaperDownloadListener
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DefaultLiveWallpaperDownloader @Inject constructor() : LiveWallpaperDownloader {

    private val _isDownloaderReady = MutableStateFlow(false)
    override val isDownloaderReady: Flow<Boolean> = _isDownloaderReady.asStateFlow()

    override fun initiateDownloadableService(
        activity: Activity,
        wallpaperData: WallpaperModel.StaticWallpaperModel,
        intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        _isDownloaderReady.value = true
    }

    override fun cleanup() {}

    override fun downloadWallpaper(listener: LiveWallpaperDownloadListener) {}

    override fun cancelDownloadWallpaper(): Boolean = false
}
