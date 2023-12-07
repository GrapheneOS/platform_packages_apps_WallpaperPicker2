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

import com.android.wallpaper.picker.preview.domain.interactor.PreviewActionsInteractor
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.CUSTOMIZE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DELETE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DOWNLOAD
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EDIT
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EFFECTS
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.INFORMATION
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.SHARE
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.InfoFloatingSheetViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/** View model for the preview action buttons */
@ViewModelScoped
class PreviewActionsViewModel
@Inject
constructor(
    interactor: PreviewActionsInteractor,
) {
    /** Action's isVisible state */
    private val _isInformationVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isInformationVisible: Flow<Boolean> = _isInformationVisible.asStateFlow()

    private val _isDownloadVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDownloadVisible: Flow<Boolean> = _isDownloadVisible.asStateFlow()

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

    /** This flow emits the view data for the info button bottom sheet */
    val infoButtonAndFloatingSheetViewModel: Flow<InfoFloatingSheetViewModel> =
        interactor.wallpaperModel.filterNotNull().map { wallpaperModel ->
            InfoFloatingSheetViewModel(wallpaperModel)
        }

    /** This flow expresses the visibility state of the info button */
    val showInfoButton: Flow<Boolean> =
        interactor.wallpaperModel.map { wallpaperModel -> wallpaperModel != null }
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
