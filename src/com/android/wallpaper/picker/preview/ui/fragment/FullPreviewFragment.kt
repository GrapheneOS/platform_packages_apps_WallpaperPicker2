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
package com.android.wallpaper.picker.preview.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.wallpaper.R
import com.android.wallpaper.dispatchers.MainDispatcher
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.preview.ui.binder.CropWallpaperButtonBinder
import com.android.wallpaper.picker.preview.ui.binder.FullWallpaperPreviewBinder
import com.android.wallpaper.picker.preview.ui.binder.WorkspacePreviewBinder
import com.android.wallpaper.picker.preview.ui.view.FullPreviewFrameLayout
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/** Shows full preview of user selected wallpaper for cropping, zooming and positioning. */
@AndroidEntryPoint(AppbarFragment::class)
class FullPreviewFragment : Hilt_FullPreviewFragment() {

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var displayUtils: DisplayUtils
    @Inject @MainDispatcher lateinit var mainScope: CoroutineScope

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_full_preview, container, false)
        setUpToolbar(view)

        val wallpaperPreviewCrop: FullPreviewFrameLayout =
            view.requireViewById(R.id.wallpaper_preview_crop)
        wallpaperPreviewViewModel.selectedSmallPreviewConfig?.let {
            wallpaperPreviewCrop.setCurrentAndTargetDisplaySize(
                displayUtils.getRealSize(checkNotNull(view.context.display)),
                it.displaySize
            )
        }

        FullWallpaperPreviewBinder.bind(
            appContext,
            view.requireViewById(R.id.wallpaper_surface),
            view.requireViewById(R.id.touch_forwarding_layout),
            wallpaperPreviewViewModel,
            viewLifecycleOwner,
            mainScope,
        )

        CropWallpaperButtonBinder.bind(
            view.requireViewById(R.id.crop_wallpaper_button),
            wallpaperPreviewViewModel,
        ) {
            findNavController().popBackStack()
        }

        wallpaperPreviewViewModel.selectedWorkspacePreviewConfig?.let { config ->
            WorkspacePreviewBinder.bind(view.requireViewById(R.id.workspace_surface), config)
        }

        return view
    }

    // TODO(b/291761856): Use real string
    override fun getDefaultTitle(): CharSequence {
        return ""
    }

    override fun getToolbarColorId(): Int {
        return android.R.color.transparent
    }

    override fun getToolbarTextColor(): Int {
        return ContextCompat.getColor(requireContext(), R.color.system_on_surface)
    }
}
