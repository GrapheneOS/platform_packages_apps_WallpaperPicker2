/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.picker.customization.ui.binder

import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.LifecycleOwner
import com.android.wallpaper.picker.customization.ui.view.PackThemeSuggestedChip
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2

object SuggestedEntryBinder {
    fun bind(
        view: PackThemeSuggestedChip,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val isOnMainScreen = {
            viewModel.customizationOptionsViewModel.selectedOption.value == null
        }

        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(DrawableCompat.wrap(view.suggestedChip.background), color)
            },
            color = colorUpdateViewModel.colorSecondaryContainer,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
        )
        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(DrawableCompat.wrap(view.cancelButton.background), color)
                DrawableCompat.setTint(DrawableCompat.wrap(view.icon.background), color)
                view.suggestedChipText.setTextColor(color)
            },
            color = colorUpdateViewModel.colorOnPrimaryContainer,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
        )
    }
}
