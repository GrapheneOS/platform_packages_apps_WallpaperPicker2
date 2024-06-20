/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.wallpaper.picker.preview.domain.interactor

import com.android.wallpaper.effects.Effect
import com.android.wallpaper.effects.EffectsController.EffectEnumInterface
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.data.repository.CreativeEffectsRepository
import com.android.wallpaper.picker.preview.data.repository.DownloadableWallpaperRepository
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.shared.model.DownloadableWallpaperModel
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** This class handles the business logic for Preview screen's action buttons */
@ActivityRetainedScoped
class PreviewActionsInteractor
@Inject
constructor(
    private val wallpaperPreviewRepository: WallpaperPreviewRepository,
    private val imageEffectsRepository: ImageEffectsRepository,
    private val creativeEffectsRepository: CreativeEffectsRepository,
    private val downloadableWallpaperRepository: DownloadableWallpaperRepository,
) {
    val wallpaperModel: StateFlow<WallpaperModel?> = wallpaperPreviewRepository.wallpaperModel

    val downloadableWallpaperModel: Flow<DownloadableWallpaperModel> =
        downloadableWallpaperRepository.downloadableWallpaperModel

    val imageEffectsModel = imageEffectsRepository.imageEffectsModel
    val imageEffect = imageEffectsRepository.wallpaperEffect
    val creativeEffectsModel = creativeEffectsRepository.creativeEffectsModel

    suspend fun turnOnCreativeEffect(actionPosition: Int) {
        creativeEffectsRepository.turnOnCreativeEffect(actionPosition)
    }

    fun enableImageEffect(effect: EffectEnumInterface) {
        imageEffectsRepository.enableImageEffect(effect)
    }

    fun disableImageEffect() {
        imageEffectsRepository.disableImageEffect()
    }

    fun isTargetEffect(effect: EffectEnumInterface): Boolean {
        return imageEffectsRepository.isTargetEffect(effect)
    }

    fun getEffectTextRes(): WallpaperEffectsView2.EffectTextRes {
        return imageEffectsRepository.getEffectTextRes()
    }

    fun downloadWallpaper() {
        downloadableWallpaperRepository.downloadWallpaper { viewModel ->
            // If download success, update wallpaper preview repo's WallpaperModel to render the
            // live wallpaper.
            wallpaperPreviewRepository.setWallpaperModel(viewModel)
        }
    }

    fun cancelDownloadWallpaper(): Boolean =
        downloadableWallpaperRepository.cancelDownloadWallpaper()

    fun startEffectsModelDownload(effect: Effect) {
        imageEffectsRepository.startEffectsModelDownload(effect)
    }

    fun interruptEffectsModelDownload(effect: Effect) {
        imageEffectsRepository.interruptEffectsModelDownload(effect)
    }
}
