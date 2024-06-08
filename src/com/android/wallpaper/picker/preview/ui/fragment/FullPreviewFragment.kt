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
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.transition.Transition
import com.android.wallpaper.R
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.preview.ui.binder.CropWallpaperButtonBinder
import com.android.wallpaper.picker.preview.ui.binder.FullWallpaperPreviewBinder
import com.android.wallpaper.picker.preview.ui.binder.PreviewTooltipBinder
import com.android.wallpaper.picker.preview.ui.binder.WorkspacePreviewBinder
import com.android.wallpaper.picker.preview.ui.transition.ChangeScaleAndPosition
import com.android.wallpaper.picker.preview.ui.util.AnimationUtil
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Shows full preview of user selected wallpaper for cropping, zooming and positioning. */
@AndroidEntryPoint(AppbarFragment::class)
class FullPreviewFragment : Hilt_FullPreviewFragment() {

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var displayUtils: DisplayUtils

    private lateinit var currentView: View

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()
    private var useLightToolbar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = AnimationUtil.getFastFadeInTransition()
        returnTransition = AnimationUtil.getFastFadeOutTransition()
        sharedElementEnterTransition = ChangeScaleAndPosition()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        currentView = inflater.inflate(R.layout.fragment_full_preview, container, false)
        setUpToolbar(currentView, true, true)

        val previewCard: CardView = currentView.requireViewById(R.id.preview_card)
        ViewCompat.setTransitionName(
            previewCard,
            SmallPreviewFragment.FULL_PREVIEW_SHARED_ELEMENT_ID
        )

        CropWallpaperButtonBinder.bind(
            button = currentView.requireViewById(R.id.crop_wallpaper_button),
            viewModel = wallpaperPreviewViewModel,
            lifecycleOwner = viewLifecycleOwner,
        ) {
            findNavController().popBackStack()
        }

        WorkspacePreviewBinder.bindFullWorkspacePreview(
            surface = currentView.requireViewById(R.id.workspace_surface),
            viewModel = wallpaperPreviewViewModel,
            lifecycleOwner = viewLifecycleOwner,
        )

        PreviewTooltipBinder.bindFullPreviewTooltip(
            tooltipStub = currentView.requireViewById(R.id.tooltip_stub),
            viewModel = wallpaperPreviewViewModel.fullTooltipViewModel,
            lifecycleOwner = viewLifecycleOwner,
        )

        return currentView
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        var isFirstBinding = false
        if (savedInstanceState == null) {
            isFirstBinding = true
        }

        FullWallpaperPreviewBinder.bind(
            applicationContext = appContext,
            view = currentView,
            viewModel = wallpaperPreviewViewModel,
            transition = sharedElementEnterTransition as? Transition,
            displayUtils = displayUtils,
            lifecycleOwner = viewLifecycleOwner,
            savedInstanceState = savedInstanceState,
            isFirstBinding = isFirstBinding,
        ) { isFullScreen ->
            useLightToolbar = isFullScreen
            setUpToolbar(view)
        }
    }

    // TODO(b/291761856): Use real string
    override fun getDefaultTitle(): CharSequence {
        return ""
    }

    override fun getToolbarTextColor(): Int {
        return if (useLightToolbar) {
            ContextCompat.getColor(requireContext(), android.R.color.system_on_primary_light)
        } else {
            ContextCompat.getColor(requireContext(), R.color.system_on_surface)
        }
    }

    override fun isStatusBarLightText(): Boolean {
        return requireContext().resources.getBoolean(R.bool.isFragmentStatusBarLightText) or
            useLightToolbar
    }
}
