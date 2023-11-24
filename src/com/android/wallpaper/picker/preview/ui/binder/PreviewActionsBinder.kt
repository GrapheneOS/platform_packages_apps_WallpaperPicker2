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
import android.widget.CompoundButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewActionsViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.InfoFloatingSheetViewModel
import com.android.wallpaper.widget.FloatingSheet
import com.android.wallpaper.widget.WallpaperControlButtonGroup
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperModelInfoContent
import kotlinx.coroutines.launch

/** Binds the action buttons and bottom sheet to [PreviewActionsViewModel] */
object PreviewActionsBinder {
    fun bind(
        applicationContext: Context,
        previewActionsViewModel: PreviewActionsViewModel,
        lifecycleOwner: LifecycleOwner,
        wallpaperControlButtonGroup: WallpaperControlButtonGroup,
        floatingSheet: FloatingSheet,
    ) {

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    previewActionsViewModel.infoButtonAndFloatingSheetViewModel.collect {
                        instantiateInfoBottomSheet(applicationContext, it, floatingSheet)
                    }
                }

                launch {
                    previewActionsViewModel.showInfoButton.collect { shouldShowInfoButton ->
                        instantiateInfoButton(
                            floatingSheet,
                            wallpaperControlButtonGroup,
                            shouldShowInfoButton
                        )
                    }
                }
            }
        }
    }

    private fun instantiateInfoButton(
        floatingSheet: FloatingSheet,
        wallpaperControlButtonGroup: WallpaperControlButtonGroup,
        shouldShowInfoButton: Boolean
    ) {
        if (!shouldShowInfoButton) {
            return
        }
        wallpaperControlButtonGroup.showButton(
            WallpaperControlButtonGroup.INFORMATION,
            CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean
                ->
                handleInformationControlButtonCheckedChange(
                    floatingSheet,
                    wallpaperControlButtonGroup,
                    isChecked
                )
            }
        )
    }

    private fun handleInformationControlButtonCheckedChange(
        floatingSheet: FloatingSheet,
        wallpaperControlButtonGroup: WallpaperControlButtonGroup,
        isChecked: Boolean
    ) {
        if (isChecked) {
            wallpaperControlButtonGroup.deselectOtherFloatingSheetControlButtons(
                WallpaperControlButtonGroup.INFORMATION
            )
            if (floatingSheet.isFloatingSheetCollapsed) {
                floatingSheet.updateContentView(FloatingSheet.INFORMATION)
                floatingSheet.expand()
            } else {
                floatingSheet.updateContentViewWithAnimation(FloatingSheet.INFORMATION)
            }
        } else if (!wallpaperControlButtonGroup.isFloatingSheetControlButtonSelected()) {
            floatingSheet.collapse()
        }
    }

    private fun instantiateInfoBottomSheet(
        applicationContext: Context,
        infoFloatingSheetViewModel: InfoFloatingSheetViewModel?,
        floatingSheet: FloatingSheet,
    ) {
        infoFloatingSheetViewModel.let { infoFloatingSheetViewModel ->
            floatingSheet.putFloatingSheetContent(
                FloatingSheet.INFORMATION,
                WallpaperModelInfoContent(applicationContext, infoFloatingSheetViewModel)
            )
        }
    }
}
