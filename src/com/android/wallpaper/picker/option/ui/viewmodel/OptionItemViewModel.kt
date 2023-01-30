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
 *
 */

package com.android.wallpaper.picker.option.ui.viewmodel

import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import kotlinx.coroutines.flow.Flow

/** Models UI state for an item in a list of selectable options. */
data class OptionItemViewModel(
    /**
     * A stable key that uniquely identifies this option amongst all other options in the same list
     * of options.
     */
    val key: Flow<String>,

    /** An icon to show. */
    val icon: Icon,

    /**
     * A text to show to the user (or attach as content description on the icon, if there's no
     * dedicated view for it).
     */
    val text: Text,

    /** Whether this option is selected. */
    val isSelected: Flow<Boolean>,

    /** Whether this option is enabled. */
    val isEnabled: Boolean = true,

    /** Notifies that the option has been clicked by the user. */
    val onClicked: Flow<(() -> Unit)?>,

    /** Notifies that the option has been long-clicked by the user. */
    val onLongClicked: (() -> Unit)? = null,
)
