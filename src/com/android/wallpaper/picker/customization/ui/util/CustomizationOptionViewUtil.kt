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

package com.android.wallpaper.picker.customization.ui.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsData

/**
 * This util creates the views for the customization options like Clock, Shortcuts and App icon,
 * etc. We separate [CustomizationOptionViewUtil] and [CustomizationOptionUtil] since the view
 * related functions may need references Activity references and are better scoped under the
 * Activity to prevent leaking.
 */
interface CustomizationOptionViewUtil {

    fun getOptionEntries(
        customizationOptionsData: CustomizationOptionsData,
        screen: Screen,
        optionContainer: LinearLayout,
        layoutInflater: LayoutInflater,
    ): List<Pair<CustomizationOption, View>>

    fun initFloatingSheet(
        customizationOptionsData: CustomizationOptionsData,
        bottomSheetContainer: FrameLayout,
        layoutInflater: LayoutInflater,
    ): Map<CustomizationOption, View>

    fun createClockPreviewAndAddToParent(
        parentView: ViewGroup,
        layoutInflater: LayoutInflater,
    ): View?
}
