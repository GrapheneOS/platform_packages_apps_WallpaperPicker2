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

import com.android.wallpaper.effects.Effect
import com.android.wallpaper.effects.EffectsController
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.shared.model.ImageEffectsModel
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2
import kotlinx.coroutines.flow.StateFlow

interface ImageEffectsRepository {
    enum class EffectStatus {
        EFFECT_DISABLE,
        EFFECT_READY,
        EFFECT_DOWNLOAD_READY,
        EFFECT_DOWNLOAD_IN_PROGRESS,
        EFFECT_APPLY_IN_PROGRESS,
        EFFECT_APPLIED,
        EFFECT_DOWNLOAD_FAILED,
        EFFECT_APPLY_FAILED,
    }

    val imageEffectsModel: StateFlow<ImageEffectsModel>
    val wallpaperEffect: StateFlow<Effect?>

    fun areEffectsAvailable(): Boolean

    suspend fun initializeEffect(
        staticWallpaperModel: WallpaperModel.StaticWallpaperModel,
        onWallpaperModelUpdated: (wallpaper: WallpaperModel) -> Unit
    )

    fun enableImageEffect(effect: EffectsController.EffectEnumInterface)

    fun disableImageEffect()

    fun destroy() {}

    fun isTargetEffect(effect: EffectsController.EffectEnumInterface): Boolean

    fun getEffectTextRes(): WallpaperEffectsView2.EffectTextRes

    fun startEffectsModelDownload(effect: Effect)

    fun interruptEffectsModelDownload(effect: Effect)
}
