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

package com.android.wallpaper.picker.preview.ui.viewmodel

import com.android.wallpaper.model.wallpaper.WallpaperModel
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.picker.preview.domain.interactor.PreviewActionsInteractor
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.CUSTOMIZE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DELETE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DOWNLOAD
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EDIT
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EFFECTS
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.INFORMATION
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.SHARE
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.InformationFloatingSheetViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** View model for the preview action buttons */
@ViewModelScoped
class PreviewActionsViewModel
@Inject
constructor(
    interactor: PreviewActionsInteractor,
    @MainDispatcher private val scope: CoroutineScope,
) {
    private val _informationFloatingSheetViewModel: Flow<InformationFloatingSheetViewModel?> =
        interactor.wallpaperModel.map { wallpaperModel ->
            if (wallpaperModel == null || !wallpaperModel.shouldShowInformationFloatingSheet()) {
                null
            } else {
                InformationFloatingSheetViewModel(
                    wallpaperModel.commonWallpaperData.attributions,
                    wallpaperModel.commonWallpaperData.exploreActionUrl,
                )
            }
        }

    /** Action's isVisible state */
    val isInformationVisible: Flow<Boolean> = _informationFloatingSheetViewModel.map { it != null }

    val isDownloadVisible: Flow<Boolean> =
        interactor.wallpaperModel.map {
            (it as? WallpaperModel.StaticWallpaperModel)?.downloadableWallpaperData != null
        }

    private val _isDeleteVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDeleteVisible: Flow<Boolean> = _isDeleteVisible.asStateFlow()

    private val _isEditVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEditVisible: Flow<Boolean> = _isEditVisible.asStateFlow()

    private val _isCustomizeVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isCustomizeVisible: Flow<Boolean> = _isCustomizeVisible.asStateFlow()

    private val _isEffectsVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEffectsVisible: Flow<Boolean> = _isEffectsVisible.asStateFlow()

    private val _isShareVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isShareVisible: Flow<Boolean> = _isShareVisible.asStateFlow()

    /** Action's isChecked state */
    private val _isInformationChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isInformationChecked: Flow<Boolean> = _isInformationChecked.asStateFlow()

    private val _isDownloadChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDownloadChecked: Flow<Boolean> = _isDownloadChecked.asStateFlow()

    private val _isDeleteChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDeleteChecked: Flow<Boolean> = _isDeleteChecked.asStateFlow()

    private val _isEditChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEditChecked: Flow<Boolean> = _isEditChecked.asStateFlow()

    private val _isCustomizeChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isCustomizeChecked: Flow<Boolean> = _isCustomizeChecked.asStateFlow()

    private val _isEffectsChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEffectsChecked: Flow<Boolean> = _isEffectsChecked.asStateFlow()

    private val _isShareChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isShareChecked: Flow<Boolean> = _isShareChecked.asStateFlow()

    /**
     * Floating sheet contents for the bottom sheet dialog. If content is null, the bottom sheet
     * should collapse, otherwise, expended.
     */
    val informationFloatingSheetViewModel: Flow<InformationFloatingSheetViewModel?> =
        combine(isInformationChecked, _informationFloatingSheetViewModel) { checked, viewModel ->
                if (checked && viewModel != null) {
                    viewModel
                } else {
                    null
                }
            }
            .distinctUntilChanged()

    /** Action listeners */
    val onInformationClicked: Flow<(() -> Unit)?> =
        combine(isInformationVisible, isInformationChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(INFORMATION)
                    }
                    _isInformationChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    val onDownloadClicked: Flow<(() -> Unit)?> =
        combine(isDownloadVisible, isDownloadChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(DOWNLOAD)
                    }
                    _isDownloadChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    val onDeleteClicked: Flow<(() -> Unit)?> =
        combine(isDeleteVisible, isDeleteChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(DELETE)
                    }
                    _isDeleteChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    val onEditClicked: Flow<(() -> Unit)?> =
        combine(isEditVisible, isEditChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(EDIT)
                    }
                    _isEditChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    val onCustomizeClicked: Flow<(() -> Unit)?> =
        combine(isCustomizeVisible, isCustomizeChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(CUSTOMIZE)
                    }
                    _isCustomizeChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    val onEffectsClicked: Flow<(() -> Unit)?> =
        combine(isEffectsVisible, isEffectsChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(EFFECTS)
                    }
                    _isEffectsChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    val onShareClicked: Flow<(() -> Unit)?> =
        combine(isShareVisible, isShareChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(SHARE)
                    }
                    _isShareChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    private fun uncheckAllOthersExcept(action: Action) {
        if (action != INFORMATION) {
            _isInformationChecked.value = false
        }
        if (action != DOWNLOAD) {
            _isDownloadChecked.value = false
        }
        if (action != DELETE) {
            _isDeleteChecked.value = false
        }
        if (action != EDIT) {
            _isEditChecked.value = false
        }
        if (action != CUSTOMIZE) {
            _isCustomizeChecked.value = false
        }
        if (action != EFFECTS) {
            _isEffectsChecked.value = false
        }
        if (action != SHARE) {
            _isShareChecked.value = false
        }
    }

    fun onDialogCollapsed() {
        if (_isInformationChecked.value) {
            _isInformationChecked.value = false
        }
    }

    companion object {
        private fun WallpaperModel.shouldShowInformationFloatingSheet(): Boolean {
            return if (
                commonWallpaperData.attributions.isNullOrEmpty() &&
                    commonWallpaperData.exploreActionUrl.isNullOrEmpty()
            ) {
                // If neither of the attributes nor the action url exists, do not show the
                // information floating sheet.
                false
            } else if (
                this is WallpaperModel.LiveWallpaperModel &&
                    !liveWallpaperData.systemWallpaperInfo.showMetadataInPreview
            ) {
                // If the live wallpaper's flag of showMetadataInPreview is false, do not show the
                // information floating sheet.
                false
            } else {
                true
            }
        }
    }
}

enum class Action {
    INFORMATION,
    DOWNLOAD,
    DELETE,
    EDIT,
    CUSTOMIZE,
    EFFECTS,
    SHARE,
}
