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

import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.preview.ui.view.PreviewActionFloatingSheet
import com.android.wallpaper.picker.preview.ui.view.PreviewActionGroup
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.CUSTOMIZE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DELETE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DOWNLOAD
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EDIT
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EFFECTS
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.INFORMATION
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.SHARE
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewActionsViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import kotlinx.coroutines.launch

/** Binds the action buttons and bottom sheet to [PreviewActionsViewModel] */
object PreviewActionsBinder {
    fun bind(
        actionGroup: PreviewActionGroup,
        floatingSheet: PreviewActionFloatingSheet,
        viewModel: PreviewActionsViewModel,
        lifecycleOwner: LifecycleOwner,
        logger: UserEventLogger,
    ) {
        val floatingSheetCallback =
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(view: View, newState: Int) {
                    if (newState == STATE_HIDDEN) {
                        viewModel.onDialogCollapsed()
                    }
                }

                override fun onSlide(p0: View, p1: Float) {}
            }
        floatingSheet.addFloatingSheetCallback(floatingSheetCallback)
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                floatingSheet.addFloatingSheetCallback(floatingSheetCallback)
            }
            floatingSheet.removeFloatingSheetCallback(floatingSheetCallback)
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isInformationChecked.collect {
                        actionGroup.setIsChecked(INFORMATION, it)
                    }
                }

                launch {
                    viewModel.isInformationVisible.collect {
                        actionGroup.setIsVisible(INFORMATION, it)
                    }
                }

                launch {
                    viewModel.onInformationClicked.collect {
                        actionGroup.setClickListener(INFORMATION, it)
                    }
                }

                launch {
                    viewModel.isDownloadChecked.collect { actionGroup.setIsChecked(DOWNLOAD, it) }
                }

                launch {
                    viewModel.isDownloadVisible.collect { actionGroup.setIsVisible(DOWNLOAD, it) }
                }

                launch {
                    viewModel.onDownloadClicked.collect {
                        actionGroup.setClickListener(DOWNLOAD, it)
                    }
                }

                launch {
                    viewModel.isDeleteChecked.collect { actionGroup.setIsChecked(DELETE, it) }
                }

                launch {
                    viewModel.isDeleteVisible.collect { actionGroup.setIsVisible(DELETE, it) }
                }

                launch {
                    viewModel.onDeleteClicked.collect { actionGroup.setClickListener(DELETE, it) }
                }

                launch { viewModel.isEditChecked.collect { actionGroup.setIsChecked(EDIT, it) } }

                launch { viewModel.isEditVisible.collect { actionGroup.setIsVisible(EDIT, it) } }

                launch {
                    viewModel.onEditClicked.collect { actionGroup.setClickListener(EDIT, it) }
                }

                launch {
                    viewModel.isCustomizeChecked.collect { actionGroup.setIsChecked(CUSTOMIZE, it) }
                }

                launch {
                    viewModel.isCustomizeVisible.collect { actionGroup.setIsVisible(CUSTOMIZE, it) }
                }

                launch {
                    viewModel.onCustomizeClicked.collect {
                        actionGroup.setClickListener(CUSTOMIZE, it)
                    }
                }

                launch {
                    viewModel.isEffectsChecked.collect { actionGroup.setIsChecked(EFFECTS, it) }
                }

                launch {
                    viewModel.isEffectsVisible.collect { actionGroup.setIsVisible(EFFECTS, it) }
                }

                launch {
                    viewModel.onEffectsClicked.collect { actionGroup.setClickListener(EFFECTS, it) }
                }

                launch { viewModel.isShareChecked.collect { actionGroup.setIsChecked(SHARE, it) } }

                launch { viewModel.isShareVisible.collect { actionGroup.setIsVisible(SHARE, it) } }

                launch {
                    viewModel.onShareClicked.collect { actionGroup.setClickListener(SHARE, it) }
                }

                launch {
                    viewModel.informationFloatingSheetViewModel.collect { viewModel ->
                        if (viewModel == null) {
                            floatingSheet.collapse()
                        } else {
                            val onExploreButtonClicked =
                                viewModel.exploreActionUrl?.let { url ->
                                    {
                                        logger.logWallpaperExploreButtonClicked()
                                        val appContext = floatingSheet.context.applicationContext
                                        appContext.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        )
                                    }
                                }
                            floatingSheet.setInformationContent(
                                viewModel.attributions,
                                onExploreButtonClicked
                            )
                            floatingSheet.expand()
                        }
                    }
                }
            }
        }
    }
}
