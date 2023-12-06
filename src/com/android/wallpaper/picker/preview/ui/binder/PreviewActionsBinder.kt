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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.picker.preview.ui.view.PreviewActionGroup
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.CUSTOMIZE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DELETE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DOWNLOAD
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EDIT
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EFFECTS
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.INFORMATION
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.SHARE
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewActionsViewModel
import kotlinx.coroutines.launch

/** Binds the action buttons and bottom sheet to [PreviewActionsViewModel] */
object PreviewActionsBinder {
    fun bind(
        view: PreviewActionGroup,
        viewModel: PreviewActionsViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isInformationChecked.collect { view.setIsChecked(INFORMATION, it) }
                }

                launch {
                    viewModel.isInformationVisible.collect { view.setIsVisible(INFORMATION, it) }
                }

                launch {
                    viewModel.onInformationClicked.collect {
                        view.setClickListener(INFORMATION, it)
                    }
                }

                launch { viewModel.isDownloadChecked.collect { view.setIsChecked(DOWNLOAD, it) } }

                launch { viewModel.isDownloadVisible.collect { view.setIsVisible(DOWNLOAD, it) } }

                launch {
                    viewModel.onDownloadClicked.collect { view.setClickListener(DOWNLOAD, it) }
                }

                launch { viewModel.isDeleteChecked.collect { view.setIsChecked(DELETE, it) } }

                launch { viewModel.isDeleteVisible.collect { view.setIsVisible(DELETE, it) } }

                launch { viewModel.onDeleteClicked.collect { view.setClickListener(DELETE, it) } }

                launch { viewModel.isEditChecked.collect { view.setIsChecked(EDIT, it) } }

                launch { viewModel.isEditVisible.collect { view.setIsVisible(EDIT, it) } }

                launch { viewModel.onEditClicked.collect { view.setClickListener(EDIT, it) } }

                launch { viewModel.isCustomizeChecked.collect { view.setIsChecked(CUSTOMIZE, it) } }

                launch { viewModel.isCustomizeVisible.collect { view.setIsVisible(CUSTOMIZE, it) } }

                launch {
                    viewModel.onCustomizeClicked.collect { view.setClickListener(CUSTOMIZE, it) }
                }

                launch { viewModel.isEffectsChecked.collect { view.setIsChecked(EFFECTS, it) } }

                launch { viewModel.isEffectsVisible.collect { view.setIsVisible(EFFECTS, it) } }

                launch { viewModel.onEffectsClicked.collect { view.setClickListener(EFFECTS, it) } }

                launch { viewModel.isShareChecked.collect { view.setIsChecked(SHARE, it) } }

                launch { viewModel.isShareVisible.collect { view.setIsVisible(SHARE, it) } }

                launch { viewModel.onShareClicked.collect { view.setClickListener(SHARE, it) } }
            }
        }
    }
}
