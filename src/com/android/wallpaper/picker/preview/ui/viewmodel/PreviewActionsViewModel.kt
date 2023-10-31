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
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.InfoFloatingSheetViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/** View model for preview action buttons [SmallPreviewFragment] */
@ViewModelScoped
class PreviewActionsViewModel
@Inject
constructor(
    private val previewActionsInteractor: PreviewActionsInteractor,
) {

    /**
     * The info button is always visible unless in the anomalous case the wallpaper model is null
     */
    val infoButtonAndFloatingSheetViewModel: Flow<InfoFloatingSheetViewModel> =
        previewActionsInteractor.wallpaperModel.filterNotNull().map { wallpaperModel ->
            InfoFloatingSheetViewModel(wallpaperModel)
        }

    val showInfoButton: Flow<Boolean> =
        previewActionsInteractor.wallpaperModel.map { wallpaperModel -> wallpaperModel == null }
}
