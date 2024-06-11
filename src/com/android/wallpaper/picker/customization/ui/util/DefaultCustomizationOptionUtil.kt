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

package com.android.wallpaper.picker.customization.ui.util

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class DefaultCustomizationOptionUtil @Inject constructor() : CustomizationOptionUtil {

    enum class DefaultLockCustomizationOption : CustomizationOption {
        WALLPAPER,
    }

    enum class DefaultHomeCustomizationOption : CustomizationOption {
        WALLPAPER,
    }

    private var viewMap: Map<CustomizationOption, View>? = null

    override fun getOptionEntries(
        screen: Screen,
        optionContainer: LinearLayout,
        layoutInflater: LayoutInflater,
    ): List<Pair<CustomizationOption, View>> =
        when (screen) {
            LOCK_SCREEN ->
                listOf(
                    DefaultLockCustomizationOption.WALLPAPER to
                        layoutInflater.inflate(
                            R.layout.customization_option_entry_wallpaper,
                            optionContainer,
                            false
                        )
                )
            HOME_SCREEN ->
                listOf(
                    DefaultHomeCustomizationOption.WALLPAPER to
                        layoutInflater.inflate(
                            R.layout.customization_option_entry_wallpaper,
                            optionContainer,
                            false
                        )
                )
        }

    override fun initBottomSheetContent(
        bottomSheetContainer: FrameLayout,
        layoutInflater: LayoutInflater
    ) {
        viewMap = mapOf()
    }

    override fun getBottomSheetContent(option: CustomizationOption): View? {
        return viewMap?.get(option)
    }

    override fun onDestroy() {
        viewMap = null
    }
}
