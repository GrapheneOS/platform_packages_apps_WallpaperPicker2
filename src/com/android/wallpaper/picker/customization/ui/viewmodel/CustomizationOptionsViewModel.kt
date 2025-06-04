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

package com.android.wallpaper.picker.customization.ui.viewmodel

import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CustomizationOptionsViewModel {

    val customizationOptionsData: Flow<CustomizationOptionsData>

    val wallpaperCarouselViewModel: WallpaperCarouselViewModel

    val selectedOption: StateFlow<CustomizationOptionUtil.CustomizationOption?>

    /**
     * View model for the discard changes dialog. The dialog shows when users leave with uncommitted
     * customization changes. When null, the dialog should hide or dismiss; otherwise, the dialog
     * should show on the screen.
     */
    val discardChangesDialogViewModel: Flow<DiscardChangesDialogViewModel?>

    /**
     * Handle back pressed. [CustomizationOptionsViewModel] should deselect the selected option and
     * return true. If no option is selected, do nothing and return false.
     *
     * @return True if back pressed is handled by [CustomizationOptionsViewModel]
     */
    fun handleBackPressed(): Boolean

    /** Reset all the customization options that are being previewed. */
    fun resetPreview()

    /** When the transition to a secondary customization option screen completes. */
    fun onTransitionToSecondaryScreenComplete()
}

interface CustomizationOptionsViewModelFactory {

    /**
     * @param initialDeepLinkDestination This field will only be not null, when receiving an intent
     *   requiring to deep-link to screens other than the main screen.
     * @param initialDeepLinkShortcutSlotId In ths case of deep-linking, this is the initial
     *   shortcut slot ID where the correspondent slot tab needs to be selected.
     */
    fun create(
        viewModelScope: CoroutineScope,
        initialDeepLinkDestination: String?,
        initialDeepLinkShortcutSlotId: String?,
    ): CustomizationOptionsViewModel
}
