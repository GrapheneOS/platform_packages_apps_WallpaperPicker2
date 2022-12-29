/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.wallpaper.config

import android.content.Context
import android.os.SystemProperties
import com.android.systemui.shared.customization.data.content.CustomizationProviderClientImpl
import com.android.systemui.shared.customization.data.content.CustomizationProviderContract as Contract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

abstract class BaseFlags {
    open fun isStagingBackdropContentEnabled() = false
    open fun isEnableWallpaperEffect() = false
    fun isMonochromaticFlagEnabled() =
        SystemProperties.getBoolean("persist.sysui.monochromatic", false)
    open fun isEnableEffectOnMultiplePanel() = false
    open fun isFullscreenWallpaperPreview() = false
    fun isUseRevampedUi(context: Context): Boolean {
        return runBlocking { CustomizationProviderClientImpl(context, Dispatchers.IO).queryFlags() }
            .firstOrNull { flag ->
                flag.name == Contract.FlagsTable.FLAG_NAME_REVAMPED_WALLPAPER_UI
            }
            ?.value == true
    }
}
