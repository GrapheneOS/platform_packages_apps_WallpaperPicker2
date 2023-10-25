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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.wallpaper.R
import com.android.wallpaper.dispatchers.MainDispatcher
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.preview.ui.binder.CropWallpaperButtonBinder
import com.android.wallpaper.picker.preview.ui.binder.FullWallpaperPreviewBinder
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewSurfaceViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.RtlUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/** Shows full preview of user selected wallpaper for cropping, zooming and positioning. */
@AndroidEntryPoint(AppbarFragment::class)
class FullPreviewFragment : Hilt_FullPreviewFragment() {

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

        val appContext = requireContext().applicationContext
        FullWallpaperPreviewBinder.bind(
            appContext,
            view.requireViewById(R.id.wallpaper_surface),
            view.requireViewById(R.id.touch_forwarding_layout),
            FullPreviewSurfaceViewModel(
                previewTransitionViewModel =
                    checkNotNull(wallpaperPreviewViewModel.previewTransitionViewModel),
                currentDisplaySize = displayUtils.getRealSize(checkNotNull(view.context.display))
            ),
            wallpaperPreviewViewModel,
            viewLifecycleOwner,
            mainScope,
            displayUtils.isSingleDisplayOrUnfoldedHorizontalHinge(requireActivity()),
            RtlUtils.isRtl(requireContext().applicationContext),
            staticPreviewView =
                if (checkNotNull(wallpaperPreviewViewModel.editingWallpaper) is LiveWallpaperInfo) {
                    null
                } else {
                    LayoutInflater.from(appContext)
                        .inflate(R.layout.fullscreen_wallpaper_preview, null)
                },
        )

        CropWallpaperButtonBinder.bind(
            view.requireViewById(R.id.crop_wallpaper_button),
        ) {
            findNavController().navigate(R.id.action_fullPreviewFragment_to_smallPreviewFragment)
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
