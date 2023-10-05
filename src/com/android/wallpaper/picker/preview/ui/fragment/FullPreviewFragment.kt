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
import com.android.wallpaper.R
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.preview.ui.binder.FullPreviewSurfaceViewBinder
import com.android.wallpaper.picker.preview.ui.binder.StaticWallpaperPreviewBinder
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewSurfaceViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.RtlUtils
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Shows full preview of user selected wallpaper for cropping, zooming and positioning. */
@AndroidEntryPoint(AppbarFragment::class)
class FullPreviewFragment : Hilt_FullPreviewFragment() {

    @Inject lateinit var displayUtils: DisplayUtils

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_full_preview, container, false)
        val previewContainer =
            inflater.inflate(R.layout.fullscreen_wallpaper_preview, container, false)
        val fullResImageView =
            previewContainer.requireViewById<SubsamplingScaleImageView>(R.id.full_res_image)
        setUpToolbar(view)

        FullPreviewSurfaceViewBinder.bind(
            surfaceView = view.requireViewById(R.id.wallpaper_surface),
            surfaceViewModel =
                FullPreviewSurfaceViewModel(
                    previewTransitionViewModel =
                        checkNotNull(wallpaperPreviewViewModel.previewTransitionViewModel),
                    currentDisplaySize =
                        displayUtils.getRealSize(checkNotNull(view.context.display))
                ),
            viewHierarchyContainer = previewContainer,
            surfaceTouchForwardingLayout = view.requireViewById(R.id.touch_forwarding_layout),
            touchRecipientView = fullResImageView,
            viewLifecycleOwner = viewLifecycleOwner,
        )
        StaticWallpaperPreviewBinder.bind(
            fullResImageView = fullResImageView,
            lowResImageView = previewContainer.requireViewById(R.id.low_res_image),
            viewModel = wallpaperPreviewViewModel.getStaticWallpaperPreviewViewModel(),
            viewLifecycleOwner = viewLifecycleOwner,
            isSingleDisplayOrUnfoldedHorizontalHinge =
                displayUtils.isSingleDisplayOrUnfoldedHorizontalHinge(requireActivity()),
            isRtl = RtlUtils.isRtl(requireContext().applicationContext),
        )

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
