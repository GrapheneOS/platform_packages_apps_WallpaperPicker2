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

package com.android.wallpaper.picker.common.preview.ui.binder

import android.os.Message
import androidx.lifecycle.LifecycleOwner
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultWorkspaceCallbackBinder @Inject constructor() : WorkspaceCallbackBinder {

    override fun bind(
        workspaceCallback: Message,
        viewModel: CustomizationOptionsViewModel,
        screen: Screen,
        lifecycleOwner: LifecycleOwner,
    ) {}

    companion object {
        const val MESSAGE_ID_UPDATE_PREVIEW = 1337
        const val KEY_HIDE_BOTTOM_ROW = "hide_bottom_row"
    }
}
