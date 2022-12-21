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
 *
 */

package com.android.wallpaper.picker.undo.ui.viewmodel

import com.android.wallpaper.picker.undo.domain.interactor.UndoInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Models the UI state of the undo system. */
class UndoViewModel(
    private val interactor: UndoInteractor,
) {
    /** Whether the "revert" button should be visible. */
    val isRevertButtonVisible: Flow<Boolean> = interactor.isUndoable
    private val _dialog = MutableStateFlow<UndoDialogViewModel?>(null)
    /**
     * A view-model of the undo confirmation dialog that should be shown, or `null` when no dialog
     * should be shown.
     */
    val dialog: Flow<UndoDialogViewModel?> = _dialog.asStateFlow()

    /** Notifies that the "revert" button has been clicked by the user. */
    fun onRevertButtonClicked() {
        _dialog.value =
            UndoDialogViewModel(
                onConfirmed = {
                    interactor.revertAll()
                    _dialog.value = null
                },
                onDismissed = { _dialog.value = null },
            )
    }
}
