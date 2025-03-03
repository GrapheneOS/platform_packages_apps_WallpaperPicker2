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

import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.DeviceDisplayType.Companion.FOLDABLE_DISPLAY_TYPES
import com.android.wallpaper.picker.customization.ui.binder.CustomizationPickerBinder2.ALPHA_NON_SELECTED_PREVIEW
import com.android.wallpaper.picker.customization.ui.binder.CustomizationPickerBinder2.ALPHA_SELECTED_PREVIEW
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.picker.preview.ui.view.ClickableMotionLayout
import com.android.wallpaper.picker.preview.ui.view.DualDisplayAspectRatioLayout.Companion.getViewId
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel.Companion.PreviewScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Binds the set wallpaper button on small preview. */
object ApplyWallpaperScreenBinder {

    fun bind(
        previewPager: ClickableMotionLayout,
        applyButton: Button,
        cancelButton: Button,
        homeCheckbox: CheckBox,
        lockCheckbox: CheckBox,
        viewModel: WallpaperPreviewViewModel,
        lifecycleOwner: LifecycleOwner,
        @MainDispatcher mainScope: CoroutineScope,
        isFoldable: Boolean,
        onWallpaperSet: () -> Unit,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.onCancelButtonClicked.collect { onClicked ->
                        cancelButton.setOnClickListener { onClicked() }
                    }
                }

                launch {
                    viewModel.isApplyButtonEnabled.collect {
                        applyButton.isEnabled = it
                        if (it) {
                            applyButton.background.alpha = 255 // 255 for 100% transparent
                            applyButton.setTextColor(
                                ContextCompat.getColor(
                                    applyButton.context,
                                    R.color.system_on_primary,
                                )
                            )
                            previewPager.addClickableViewId(applyButton.id)
                        } else {
                            applyButton.background.alpha = 31 // 31 for 12% transparent
                            applyButton.setTextColor(
                                ColorUtils.setAlphaComponent(
                                    ContextCompat.getColor(
                                        applyButton.context,
                                        R.color.system_on_surface,
                                    ),
                                    97, // 97 for 38% transparent
                                )
                            )
                            previewPager.removeClickableViewId(applyButton.id)
                        }
                    }
                }

                launch {
                    combine(viewModel.currentPreviewScreen, viewModel.isHomeCheckBoxChecked) {
                            screen,
                            checked ->
                            screen to checked
                        }
                        .collect { (screen, checked) ->
                            if (screen == PreviewScreen.APPLY_WALLPAPER) {
                                homeCheckbox.isChecked = checked
                                animateScreenAlpha(
                                    screenPreview = previewPager.requireViewById(R.id.home_preview),
                                    checked = checked,
                                    isFoldable = isFoldable,
                                )
                            }
                        }
                }

                launch {
                    combine(viewModel.currentPreviewScreen, viewModel.isLockCheckBoxChecked) {
                            screen,
                            checked ->
                            screen to checked
                        }
                        .collect { (screen, checked) ->
                            if (screen == PreviewScreen.APPLY_WALLPAPER) {
                                lockCheckbox.isChecked = checked
                                animateScreenAlpha(
                                    screenPreview = previewPager.requireViewById(R.id.lock_preview),
                                    checked = checked,
                                    isFoldable = isFoldable,
                                )
                            }
                        }
                }

                launch {
                    viewModel.onHomeCheckBoxChecked.collect {
                        homeCheckbox.setOnClickListener { it() }
                    }
                }

                launch {
                    viewModel.onLockCheckBoxChecked.collect {
                        lockCheckbox.setOnClickListener { it() }
                    }
                }

                launch {
                    viewModel.setWallpaperDialogOnConfirmButtonClicked.collect { onClicked ->
                        applyButton.setOnClickListener {
                            mainScope.launch {
                                onClicked()
                                onWallpaperSet()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun animateScreenAlpha(screenPreview: View, checked: Boolean, isFoldable: Boolean) {
        if (isFoldable) {
            FOLDABLE_DISPLAY_TYPES.map { screenPreview.requireViewById<View>(it.getViewId()) }
                .forEach { animatePreviewAlpha(parent = it, checked = checked) }
        } else {
            animatePreviewAlpha(parent = screenPreview, checked = checked)
        }
    }

    private fun animatePreviewAlpha(parent: View, checked: Boolean) {
        setOf(R.id.wallpaper_surface, R.id.workspace_surface).forEach {
            parent
                .requireViewById<View>(it)
                .animate()
                .alpha(if (checked) ALPHA_SELECTED_PREVIEW else ALPHA_NON_SELECTED_PREVIEW)
                .setDuration(
                    parent.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                )
                .start()
        }
    }
}
