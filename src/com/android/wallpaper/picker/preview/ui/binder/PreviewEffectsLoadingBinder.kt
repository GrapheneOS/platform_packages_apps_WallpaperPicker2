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
package com.android.wallpaper.picker.preview.ui.binder

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.systemui.monet.ColorScheme
import com.android.wallpaper.picker.customization.animation.view.LoadingAnimation
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_APPLIED
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_APPLY_FAILED
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_APPLY_IN_PROGRESS
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_DOWNLOAD_FAILED
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_READY
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.ResourceUtils
import kotlinx.coroutines.launch

object PreviewEffectsLoadingBinder {
    interface Binding {
        fun destroy()
    }

    fun bind(
        view: View,
        viewModel: WallpaperPreviewViewModel,
        viewLifecycleOwner: LifecycleOwner,
    ): Binding {
        var loadingAnimation: LoadingAnimation? = null
        val job =
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                    loadingAnimation = getLoadingAnimation(view)
                    viewModel.imageEffectsModel.collect { model ->
                        if (model.status == EFFECT_APPLY_IN_PROGRESS) {
                            loadingAnimation?.playLoadingAnimation(seed = null)
                        } else if (
                            model.status == EFFECT_APPLIED ||
                                model.status == EFFECT_READY ||
                                model.status == EFFECT_DOWNLOAD_FAILED ||
                                model.status == EFFECT_APPLY_FAILED
                        ) {
                            // Play reveal animation whether applying the effect succeeded or
                            // failed.
                            loadingAnimation?.playRevealAnimation()
                        }
                    }
                }
                loadingAnimation?.cancel()
                loadingAnimation = null
            }
        return object : Binding {
            override fun destroy() {
                job.cancel()
                loadingAnimation?.cancel()
                loadingAnimation = null
            }
        }
    }

    private fun getLoadingAnimation(view: View): LoadingAnimation {
        val context: Context = view.context
        val loadingAnimation =
            LoadingAnimation(
                revealOverlay = view,
                revealType = LoadingAnimation.RevealType.FADE,
                timeOutDuration = null
            )
        val colorAccent = ResourceUtils.getColorAttr(context, android.R.attr.colorAccent)
        val isDarkTheme =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        loadingAnimation.updateColor(ColorScheme(colorAccent, isDarkTheme))
        return loadingAnimation
    }
}
