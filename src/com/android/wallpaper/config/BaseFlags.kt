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

import android.app.WallpaperManager
import android.content.Context
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient
import com.android.systemui.shared.customization.data.content.CustomizationProviderClientImpl
import com.android.systemui.shared.customization.data.content.CustomizationProviderContract as Contract
import com.android.wallpaper.module.InjectorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

abstract class BaseFlags {
    var customizationProviderClient: CustomizationProviderClient? = null
    private var cachedFlags: List<CustomizationProviderClient.Flag>? = null
    open fun isStagingBackdropContentEnabled() = false
    open fun isWallpaperEffectEnabled() = false
    open fun isWallpaperEffectModelDownloadEnabled() = true
    open fun isInterruptModelDownloadEnabled() = false

    // TODO(b/285047815): Remove flag after adding wallpaper id for default static wallpaper
    open fun isWallpaperRestorerEnabled() = false

    /**
     * Enables new preview UI if both [isMultiCropEnabled] and this flag are true.
     *
     * TODO(b/291761856): Create SysUI flag for new preview UI
     */
    open fun isMultiCropPreviewUiEnabled() = false

    open fun isMultiCropEnabled() = WallpaperManager.isMultiCropEnabled()

    open fun isUseRevampedUiEnabled(context: Context): Boolean {
        return getCachedFlags(context)
            .firstOrNull { flag ->
                flag.name == Contract.FlagsTable.FLAG_NAME_REVAMPED_WALLPAPER_UI
            }
            ?.value == true
    }
    open fun isCustomClocksEnabled(context: Context): Boolean {
        return getCachedFlags(context)
            .firstOrNull { flag ->
                flag.name == Contract.FlagsTable.FLAG_NAME_CUSTOM_CLOCKS_ENABLED
            }
            ?.value == true
    }
    open fun isMonochromaticThemeEnabled(context: Context): Boolean {
        return getCachedFlags(context)
            .firstOrNull { flag -> flag.name == Contract.FlagsTable.FLAG_NAME_MONOCHROMATIC_THEME }
            ?.value == true
    }

    open fun isAIWallpaperEnabled(context: Context): Boolean {
        return getCachedFlags(context)
            .firstOrNull { flag ->
                flag.name == Contract.FlagsTable.FLAG_NAME_WALLPAPER_PICKER_UI_FOR_AIWP
            }
            ?.value == true
    }

    open fun isTransitClockEnabled(context: Context): Boolean {
        return getCachedFlags(context)
            .firstOrNull { flag -> flag.name == Contract.FlagsTable.FLAG_NAME_TRANSIT_CLOCK }
            ?.value == true
    }

    /**
     * This flag is to for refactoring the process of setting a wallpaper from the Wallpaper Picker,
     * such as changes in WallpaperSetter, WallpaperPersister and WallpaperPreferences.
     */
    fun isRefactorSettingWallpaper(): Boolean {
        return false
    }

    open fun isPageTransitionsFeatureEnabled(context: Context): Boolean {
        return getCachedFlags(context)
            .firstOrNull { flag -> flag.name == Contract.FlagsTable.FLAG_NAME_PAGE_TRANSITIONS }
            ?.value == true
    }

    open fun isGridApplyButtonEnabled(context: Context): Boolean {
        return getCachedFlags(context)
            .firstOrNull { flag -> flag.name == Contract.FlagsTable.FLAG_NAME_GRID_APPLY_BUTTON }
            ?.value == true
    }

    open fun isPreviewLoadingAnimationEnabled(context: Context): Boolean {
        return getCachedFlags(context)
            .firstOrNull { flag ->
                flag.name == Contract.FlagsTable.FLAG_NAME_WALLPAPER_PICKER_PREVIEW_ANIMATION
            }
            ?.value == true
    }

    private fun getCustomizationProviderClient(context: Context): CustomizationProviderClient {
        return customizationProviderClient
            ?: CustomizationProviderClientImpl(context.applicationContext, Dispatchers.IO).also {
                customizationProviderClient = it
            }
    }

    fun isQuickAffordancesEnabled(context: Context): Boolean {
        return getCachedFlags(context)
            .firstOrNull { flag ->
                flag.name ==
                    Contract.FlagsTable.FLAG_NAME_CUSTOM_LOCK_SCREEN_QUICK_AFFORDANCES_ENABLED
            }
            ?.value == true
    }

    open fun getCachedFlags(context: Context): List<CustomizationProviderClient.Flag> {
        return cachedFlags
            ?: runBlocking { getCustomizationProviderClient(context).queryFlags() }
                .also { cachedFlags = it }
    }

    companion object {
        @JvmStatic
        fun get(): BaseFlags {
            return InjectorProvider.getInjector().getFlags()
        }
    }
}
