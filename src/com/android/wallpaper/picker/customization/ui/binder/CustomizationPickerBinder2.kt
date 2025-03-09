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

import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.isInvisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.picker.customization.ui.CustomizationPickerActivity2
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.util.EmptyTransitionListener
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen.CUSTOMIZATION_OPTION
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen.MAIN
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.ui.view.ClickableMotionLayout
import kotlinx.coroutines.launch

object CustomizationPickerBinder2 {

    const val ALPHA_SELECTED_PREVIEW = 1f
    const val ALPHA_NON_SELECTED_PREVIEW = 0.4f

    /**
     * @return Callback for the [CustomizationPickerActivity2] to set
     *   [CustomizationPickerViewModel2]'s screen state to null, which infers to the main screen. We
     *   need this callback to handle the back navigation in [CustomizationPickerActivity2].
     */
    fun bind(
        view: View,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>?,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        customizationOptionsBinder: CustomizationOptionsBinder,
        lifecycleOwner: LifecycleOwner,
        navigateToPrimary: () -> Unit,
        navigateToSecondary: (screen: CustomizationOption) -> Unit,
        navigateToWallpaperCategoriesScreen: (screen: Screen) -> Unit,
        navigateToMoreLockScreenSettingsActivity: () -> Unit,
        navigateToColorContrastSettingsActivity: () -> Unit,
        navigateToLockScreenNotificationsSettingsActivity: () -> Unit,
        navigateToPreviewScreen: ((wallpaperModel: WallpaperModel) -> Unit)?,
    ) {
        val lockCustomizationOptionContainer: LinearLayout =
            view.requireViewById(R.id.lock_customization_option_container)
        val homeCustomizationOptionContainer: LinearLayout =
            view.requireViewById(R.id.home_customization_option_container)
        val previewPager: ClickableMotionLayout = view.requireViewById(R.id.preview_pager)
        previewPager.setTransitionListener(
            object : EmptyTransitionListener {

                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    val screen =
                        when (currentId) {
                            R.id.lock_preview_selected -> LOCK_SCREEN
                            R.id.home_preview_selected -> HOME_SCREEN
                            else -> return
                        }
                    viewModel.selectPreviewScreen(screen)
                }
            }
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.screen.collect { (screen, option) ->
                        when (screen) {
                            MAIN -> navigateToPrimary()
                            CUSTOMIZATION_OPTION -> option?.let(navigateToSecondary)
                        }
                    }
                }

                launch {
                    viewModel.selectedPreviewScreen.collect {
                        when (it) {
                            LOCK_SCREEN -> {
                                if (previewPager.currentState != R.id.lock_preview_selected) {
                                    previewPager.jumpToState(R.id.lock_preview_selected)
                                }
                                lockCustomizationOptionContainer.isInvisible = false
                                homeCustomizationOptionContainer.isInvisible = true
                            }
                            HOME_SCREEN -> {
                                if (previewPager.currentState != R.id.home_preview_selected) {
                                    previewPager.jumpToState(R.id.home_preview_selected)
                                }
                                lockCustomizationOptionContainer.isInvisible = true
                                homeCustomizationOptionContainer.isInvisible = false
                            }
                        }
                    }
                }
            }
        }

        WallpaperPickerEntryBinder.bind(
            view = view.requireViewById(R.id.wallpaper_picker_entry),
            viewModel = viewModel,
            colorUpdateViewModel = colorUpdateViewModel,
            lifecycleOwner = lifecycleOwner,
            navigateToWallpaperCategoriesScreen = navigateToWallpaperCategoriesScreen,
            navigateToPreviewScreen = navigateToPreviewScreen,
        )

        customizationOptionsBinder.bind(
            view,
            lockScreenCustomizationOptionEntries,
            homeScreenCustomizationOptionEntries,
            customizationOptionFloatingSheetViewMap,
            viewModel,
            colorUpdateViewModel,
            lifecycleOwner,
            navigateToMoreLockScreenSettingsActivity,
            navigateToColorContrastSettingsActivity,
            navigateToLockScreenNotificationsSettingsActivity,
        )
    }
}
