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

import com.android.wallpaper.effects.Effect
import com.android.wallpaper.effects.EffectsController
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository
import com.android.wallpaper.picker.preview.shared.model.ImageEffectsModel
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class FakeImageEffectsRepository @Inject constructor() : ImageEffectsRepository {
    override var imageEffectsModel =
        MutableStateFlow(ImageEffectsModel(ImageEffectsRepository.EffectStatus.EFFECT_DISABLE))
    override var wallpaperEffect: MutableStateFlow<Effect?> = MutableStateFlow(null)

    override fun areEffectsAvailable(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun initializeEffect(
        staticWallpaperModel: WallpaperModel.StaticWallpaperModel,
        onWallpaperModelUpdated: (wallpaper: WallpaperModel) -> Unit,
    ) {
        TODO("Not yet implemented")
    }

    override fun enableImageEffect(effect: EffectsController.EffectEnumInterface) {
        TODO("Not yet implemented")
    }

    override fun disableImageEffect() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        super.destroy()
    }

    override fun isTargetEffect(effect: EffectsController.EffectEnumInterface): Boolean {
        TODO("Not yet implemented")
    }

    override fun getEffectTextRes(): WallpaperEffectsView2.EffectTextRes {
        return WallpaperEffectsView2.EffectTextRes(
            "sample title",
            "sample failed title",
            "sample subtitle",
            "sample retry instructions",
            "sample no effect instruction"
        )
    }

    override fun startEffectsModelDownload(effect: Effect) {
        TODO("Not yet implemented")
    }

    override fun interruptEffectsModelDownload(effect: Effect) {
        TODO("Not yet implemented")
    }
}
