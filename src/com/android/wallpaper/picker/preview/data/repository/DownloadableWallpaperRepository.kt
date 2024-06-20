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

package com.android.wallpaper.picker.preview.data.repository

import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.preview.data.util.LiveWallpaperDownloader
import com.android.wallpaper.picker.preview.shared.model.DownloadStatus.DOWNLOADED
import com.android.wallpaper.picker.preview.shared.model.DownloadStatus.DOWNLOADING
import com.android.wallpaper.picker.preview.shared.model.DownloadStatus.DOWNLOAD_NOT_AVAILABLE
import com.android.wallpaper.picker.preview.shared.model.DownloadStatus.READY_TO_DOWNLOAD
import com.android.wallpaper.picker.preview.shared.model.DownloadableWallpaperModel
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

@ActivityRetainedScoped
class DownloadableWallpaperRepository
@Inject
constructor(
    private val liveWallpaperDownloader: LiveWallpaperDownloader,
) {

    private val _downloadableWallpaperModel =
        MutableStateFlow(DownloadableWallpaperModel(READY_TO_DOWNLOAD, null))
    val downloadableWallpaperModel: Flow<DownloadableWallpaperModel> =
        combine(
            _downloadableWallpaperModel.asStateFlow(),
            liveWallpaperDownloader.isDownloaderReady
        ) { model, isReady ->
            if (isReady) {
                model
            } else {
                DownloadableWallpaperModel(DOWNLOAD_NOT_AVAILABLE, null)
            }
        }

    fun downloadWallpaper(onDownloaded: (wallpaperModel: LiveWallpaperModel) -> Unit) {
        _downloadableWallpaperModel.value = DownloadableWallpaperModel(DOWNLOADING, null)
        liveWallpaperDownloader.downloadWallpaper(
            object : LiveWallpaperDownloader.LiveWallpaperDownloadListener {
                override fun onDownloadSuccess(wallpaperModel: LiveWallpaperModel) {
                    onDownloaded(wallpaperModel)
                    _downloadableWallpaperModel.value =
                        DownloadableWallpaperModel(DOWNLOADED, wallpaperModel)
                }

                override fun onDownloadFailed() {
                    _downloadableWallpaperModel.value =
                        DownloadableWallpaperModel(READY_TO_DOWNLOAD, null)
                }
            }
        )
    }

    fun cancelDownloadWallpaper(): Boolean {
        return liveWallpaperDownloader.cancelDownloadWallpaper()
    }
}
