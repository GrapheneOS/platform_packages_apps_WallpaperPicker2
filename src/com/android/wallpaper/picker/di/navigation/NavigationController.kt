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

package com.android.wallpaper.picker.di.navigation

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentActivity
import com.android.wallpaper.model.WallpaperInfo

/** Enum is used to indicate how a fragment will be added to the view hierarchy */
enum class Transition {
    /** Add the fragment */
    ADD,

    /** Replace the existing fragment */
    REPLACE
}

/**
 * This class abstracts the navigation logic for the preview screens. Concrete implementations of
 * this class should provide the navigation logic for each required screen in the Preview flow.
 */
interface NavigationController {

    /**
     * This method performs navigation to the preview screen This method should use the input
     * activity and viewId to instantiate and transition to the required preview fragment
     *
     * @param activity is the activity in which the preview fragment will reside
     * @param wallpaperInfo contains the relevant data regarding the selected wallpaper
     * @param mode specifies the mode of the preview fragment i.e. crop, view only
     * @param viewFullScreen specifies whether preview screen should be launched in full screen
     * @param viewAsHome specifies if the preview should be shown as the home page
     * @param testingModeEnabled specifies whether testing mode is enabled
     * @param viewId is the id of the view in the layout where the preview fragment should be
     *   added/replaced
     * @param transition specifies if the preview fragment should be added or replaced in the
     */
    fun navigateToPreview(
        activity: FragmentActivity,
        wallpaperInfo: WallpaperInfo,
        viewAsHome: Boolean,
        viewFullScreen: Boolean,
        testingModeEnabled: Boolean,
        @IdRes viewId: Int,
        transition: Transition,
        isAssetIdPresent: Boolean
    )
}
