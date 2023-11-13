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

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.FoldableDisplay
import com.android.wallpaper.model.wallpaper.PreviewPagerPage
import com.android.wallpaper.model.wallpaper.getScreenOrientation
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.DualPreviewViewPager
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.adapters.DualPreviewPagerAdapter
import com.android.wallpaper.picker.preview.ui.viewmodel.SmallPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WorkspacePreviewConfigViewModel
import com.android.wallpaper.picker.wallpaper.utils.DualDisplayAspectRatioLayout
import com.android.wallpaper.picker.wallpaper.utils.DualDisplayAspectRatioLayout.Companion.getViewId
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.PreviewUtils
import kotlinx.coroutines.CoroutineScope

/** Binds dual preview home screen and lock screen view pager. */
object DualPreviewPagerBinder {

    fun bind(
        dualPreviewView: DualPreviewViewPager,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        homePreviewUtils: PreviewUtils,
        lockPreviewUtils: PreviewUtils,
        applicationContext: Context,
        viewLifecycleOwner: LifecycleOwner,
        mainScope: CoroutineScope,
        displayUtils: DisplayUtils,
        navigate: () -> Unit,
    ) {
        // implement adapter for the dual preview pager
        dualPreviewView.adapter = DualPreviewPagerAdapter { view, position ->
            val dualDisplayAspectRatioLayout: DualDisplayAspectRatioLayout =
                view.requireViewById(R.id.dual_preview)
            val previewDisplays =
                mapOf(
                    FoldableDisplay.FOLDED to displayUtils.getSmallerDisplay(),
                    FoldableDisplay.UNFOLDED to displayUtils.getWallpaperDisplay(),
                )

            previewDisplays
                .mapValues { displayUtils.getRealSize(it.value) }
                .let {
                    dualDisplayAspectRatioLayout.setDisplaySizes(it)
                    dualPreviewView.setDisplaySizes(it)
                }

            FoldableDisplay.entries.forEach { display ->
                val previewDisplaySize = dualDisplayAspectRatioLayout.getPreviewDisplaySize(display)
                previewDisplaySize?.let {
                    SmallPreviewBinder.bind(
                        applicationContext = applicationContext,
                        view = dualDisplayAspectRatioLayout.requireViewById(display.getViewId()),
                        viewModel = wallpaperPreviewViewModel,
                        smallPreviewConfig =
                            SmallPreviewConfigViewModel(
                                previewTab = PreviewPagerPage.entries[position].screen,
                                displaySize = it,
                                screenOrientation = getScreenOrientation(it, display),
                            ),
                        mainScope = mainScope,
                        viewLifecycleOwner = viewLifecycleOwner,
                        workspaceConfig =
                            WorkspacePreviewConfigViewModel(
                                previewUtils =
                                    if (position == PreviewPagerPage.LOCK_PREVIEW.ordinal)
                                        lockPreviewUtils
                                    else homePreviewUtils,
                                displayId = checkNotNull(previewDisplays[display]).displayId,
                            ),
                        navigate = navigate,
                    )
                }
            }
        }
    }
}
