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
import android.server.Flags.enableThemeService
import com.android.systemui.shared.Flags.extendedWallpaperEffects
import com.android.systemui.shared.Flags.extendibleThemeManager
import com.android.systemui.shared.Flags.workspaceItemsLabelHidden
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient
import com.android.systemui.shared.customization.data.content.CustomizationProviderClientImpl
import com.android.systemui.shared.customization.data.content.CustomizationProviderContract as Contract
import com.android.wallpaper.Flags.adaptiveWallpaperFlag
import com.android.wallpaper.Flags.collapsableReorderedAiWallpapersScreen
import com.android.wallpaper.Flags.colorPickerUpdateFlag
import com.android.wallpaper.Flags.composeRefactorFlag
import com.android.wallpaper.Flags.creativeWallpaperFieldCollectionWallpaper
import com.android.wallpaper.Flags.enableAiClocks
import com.android.wallpaper.Flags.enableAndroidPhotopicker
import com.android.wallpaper.Flags.enablePackThemeEntry
import com.android.wallpaper.Flags.enableRecentWallpaperDeletion
import com.android.wallpaper.Flags.enableRecentsDeletionViaProvider
import com.android.wallpaper.Flags.fullscreenPreviewFlag
import com.android.wallpaper.Flags.fullscreenPreviewFlowFix
import com.android.wallpaper.Flags.newCreativeWallpaperCategory
import com.android.wallpaper.Flags.proactiveSuggestions
import com.android.wallpaper.Flags.refactorIndividualPickerFlag
import com.android.wallpaper.Flags.refactorWallpaperInfoFlag
import com.android.wallpaper.Flags.refactorWallpaperPreviewScreenFlag
import com.android.wallpaper.Flags.wallpaperRestorerFlag
import com.android.wallpaper.R
import com.android.wm.shell.shared.desktopmode.DesktopState
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

abstract class BaseFlags {
    private var customizationProviderClient: CustomizationProviderClient? = null
    private var cachedFlags: List<CustomizationProviderClient.Flag>? = null

    open fun isAiClocksEnabled() = enableAiClocks()

    // local flag to gate the entry points for magic portrait
    open fun isMagicPortraitEntryPointsEnabled() = true

    open fun isStagingBackdropContentEnabled() = false

    open fun isPackThemeEnabled() = enablePackThemeEntry()

    open fun isWallpaperEffectModelDownloadEnabled() = true

    open fun isInterruptModelDownloadEnabled() = false

    open fun isCollabsableSectionInAiEnabled() = collapsableReorderedAiWallpapersScreen()

    open fun isEnableRecentWallpaperDeletion() = enableRecentWallpaperDeletion()

    open fun isEnableRecentsDeletionViaProvider() = enableRecentsDeletionViaProvider()

    // local flag to enable the refactored version of IPF2
    open fun isWallpapersFragmentEnabled() = refactorIndividualPickerFlag()

    open fun isWallpaperRestorerEnabled() = wallpaperRestorerFlag()

    open fun isNewCreativeWallpaperCategoryEnabled() = newCreativeWallpaperCategory()

    open fun isCreativeWallpaperCollectionFieldEnabled() =
        creativeWallpaperFieldCollectionWallpaper()

    open fun isExtendedWallpaperEnabled() = extendedWallpaperEffects()

    open fun isExtendibleThemeManager() = extendibleThemeManager()

    open fun isComposeRefactorEnabled() = composeRefactorFlag()

    open fun isColorPickerUpdateEnabled() = colorPickerUpdateFlag()

    // Local flag to gate Compose UI under the colorPickerUpdateFlag
    open fun isColorPickerComposeEnabled() = true

    open fun isThemeServiceEnabled() = enableThemeService()

    open fun isRefactorWallpaperPreviewScreenEnabled() = refactorWallpaperPreviewScreenFlag()

    open fun isRefactorWallpaperInfoFlag() = refactorWallpaperInfoFlag()

    open fun isEnableAndroidPhotoPicker() = enableAndroidPhotopicker()

    // This is just a local flag in order to ensure right behaviour in case
    // something goes wrong with PhotoPicker integration.
    open fun isPhotoPickerEnabled() = false

    // This flag is to gate the dependency of default recents on new categories
    // fetching logic.
    open fun isRefactorWallpaperDefaults() = false

    open fun isKeyguardQuickAffordanceEnabled(context: Context): Boolean {
        return getCachedFlags(context)
            .firstOrNull { flag ->
                flag.name ==
                    Contract.FlagsTable.FLAG_NAME_CUSTOM_LOCK_SCREEN_QUICK_AFFORDANCES_ENABLED
            }
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

    open fun getCachedFlags(context: Context): List<CustomizationProviderClient.Flag> {
        return cachedFlags
            ?: runBlocking { getCustomizationProviderClient(context).queryFlags() }
                .also { cachedFlags = it }
    }

    open fun isFullscreenPreviewEnabled(context: Context): Boolean {
        return fullscreenPreviewFlag() && DesktopState.fromContext(context).canEnterDesktopMode
    }

    open fun isFullscreenPreviewFlowFixEnabled(context: Context): Boolean {
        return fullscreenPreviewFlowFix() && isFullscreenPreviewEnabled(context)
    }

    open fun shouldShowDesktopUi(context: Context): Boolean {
        // TODO: b/416024080 use a better solution than a config boolean to show desktop UI.
        return context.resources.getBoolean(R.bool.isDesktopUi)
    }

    open fun isAdaptiveWallpaperEnabled(context: Context): Boolean {
        return adaptiveWallpaperFlag() &&
            context.resources.getBoolean(R.bool.isAdaptiveWallpaperSupported)
    }

    open fun isDesktopSpecificWallpaperCollectionsEnabled() = false

    open fun isHideAppLabelEnabled(): Boolean = workspaceItemsLabelHidden()

    open fun isProactiveSuggestionsEnabled(): Boolean = proactiveSuggestions()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BaseFlagsEntryPointInjector {
        fun getBaseFlags(): BaseFlags
    }

    companion object {
        @JvmStatic
        fun get(context: Context): BaseFlags {
            return EntryPoints.get(
                    context.applicationContext,
                    BaseFlagsEntryPointInjector::class.java,
                )
                .getBaseFlags()
        }
    }
}
