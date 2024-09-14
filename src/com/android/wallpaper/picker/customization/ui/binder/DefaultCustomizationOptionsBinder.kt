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

package com.android.wallpaper.picker.customization.ui.binder

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.LifecycleOwner
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.util.DefaultCustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCustomizationOptionsBinder @Inject constructor() : CustomizationOptionsBinder {

    override fun bind(
        view: View,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>?,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val optionLockWallpaper =
            lockScreenCustomizationOptionEntries
                .find {
                    it.first ==
                        DefaultCustomizationOptionUtil.DefaultLockCustomizationOption.WALLPAPER
                }
                ?.second
        val moreWallpapersLock = optionLockWallpaper?.findViewById<TextView>(R.id.more_wallpapers)
        val optionHomeWallpaper =
            homeScreenCustomizationOptionEntries
                .find {
                    it.first ==
                        DefaultCustomizationOptionUtil.DefaultHomeCustomizationOption.WALLPAPER
                }
                ?.second
        val moreWallpapersHome = optionHomeWallpaper?.findViewById<TextView>(R.id.more_wallpapers)

        ColorUpdateBinder.bind(
            setColor = { color ->
                moreWallpapersLock?.apply {
                    setTextColor(color)
                    TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(color))
                }
            },
            color = colorUpdateViewModel.colorPrimary,
            shouldAnimate = {
                viewModel.selectedPreviewScreen.value == Screen.LOCK_SCREEN &&
                    viewModel.customizationOptionsViewModel.selectedOption.value == null
            },
            lifecycleOwner = lifecycleOwner,
        )

        ColorUpdateBinder.bind(
            setColor = { color ->
                moreWallpapersHome?.apply {
                    setTextColor(color)
                    TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(color))
                }
            },
            color = colorUpdateViewModel.colorPrimary,
            shouldAnimate = {
                viewModel.selectedPreviewScreen.value == Screen.HOME_SCREEN &&
                    viewModel.customizationOptionsViewModel.selectedOption.value == null
            },
            lifecycleOwner = lifecycleOwner,
        )
    }

    override fun bindClockPreview(
        clockHostView: View,
        viewModel: CustomizationPickerViewModel2,
        lifecycleOwner: LifecycleOwner,
        clockViewFactory: ClockViewFactory,
    ) {
        // Do nothing intended
    }
}
