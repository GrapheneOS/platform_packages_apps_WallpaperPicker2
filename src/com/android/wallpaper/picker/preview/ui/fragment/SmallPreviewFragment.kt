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
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.wallpaper.R
import com.android.wallpaper.module.CustomizationSections
import com.android.wallpaper.module.CustomizationSections.Screen
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.customization.domain.interactor.WallpaperInteractor
import com.android.wallpaper.picker.customization.ui.binder.ScreenPreviewBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ScreenPreviewViewModel
import com.android.wallpaper.picker.preview.di.modules.preview.utils.PreviewUtilsModule
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewTransitionViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.picker.wallpaper.utils.DualDisplayAspectRatioLayout
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.PreviewUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * This fragment displays the preview of the selected wallpaper on all available workspaces and
 * device displays.
 */
@AndroidEntryPoint(AppbarFragment::class)
class SmallPreviewFragment : Hilt_SmallPreviewFragment() {

    @Inject lateinit var wallpaperInteractor: WallpaperInteractor
    @Inject lateinit var displayUtils: DisplayUtils

    @PreviewUtilsModule.LockScreenPreviewUtils @Inject lateinit var lockPreviewUtils: PreviewUtils

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view =
            inflater.inflate(
                R.layout.fragment_small_preview_for_two_screens,
                container,
                /* attachToRoot= */ false
            )
        setUpToolbar(view)
        bindScreenPreview(view)

        return view
    }

    // TODO(b/291761856): Use real string
    override fun getDefaultTitle(): CharSequence {
        return "Small Preview"
    }

    override fun getToolbarColorId(): Int {
        return android.R.color.transparent
    }

    override fun getToolbarTextColor(): Int {
        return ContextCompat.getColor(requireContext(), R.color.system_on_surface)
    }

    // TODO(b/291761856): Replace placeholder preview
    private fun bindScreenPreview(view: View) {
        val dualDisplayAspectRatioView: DualDisplayAspectRatioLayout =
            view.requireViewById(R.id.dual_preview)
        dualDisplayAspectRatioView.setDisplaySizes(
            displayUtils.getRealSize(displayUtils.getSmallerDisplay()),
            displayUtils.getRealSize(displayUtils.getWallpaperDisplay())
        )
        val foldedPreviewView: CardView =
            view.requireViewById(DualDisplayAspectRatioLayout.foldedPreviewId)
        val unfoldedPreviewView: CardView =
            view.requireViewById(DualDisplayAspectRatioLayout.unfoldedPreviewId)
        ScreenPreviewBinder.bind(
            activity = requireActivity(),
            previewView = foldedPreviewView,
            viewModel =
                ScreenPreviewViewModel(
                    previewUtils = lockPreviewUtils,
                    wallpaperInfoProvider = { wallpaperPreviewViewModel.editingWallpaper },
                    wallpaperInteractor = wallpaperInteractor,
                    screen = CustomizationSections.Screen.HOME_SCREEN,
                    onPreviewClicked = {
                        findNavController()
                            .navigate(R.id.action_smallPreviewFragment_to_fullPreviewFragment)
                    }
                ),
            lifecycleOwner = viewLifecycleOwner,
            offsetToStart = false,
            onWallpaperPreviewDirty = {},
        )

        ScreenPreviewBinder.bind(
            activity = requireActivity(),
            previewView = unfoldedPreviewView,
            viewModel =
                ScreenPreviewViewModel(
                    previewUtils = lockPreviewUtils,
                    wallpaperInfoProvider = { wallpaperPreviewViewModel.editingWallpaper },
                    wallpaperInteractor = wallpaperInteractor,
                    screen = Screen.HOME_SCREEN,
                    onPreviewClicked = {
                        // TODO(b/291761856): update preview transition view model from
                        //                    [SmallPreviewFragment].
                        wallpaperPreviewViewModel.previewTransitionViewModel =
                            PreviewTransitionViewModel(
                                previewTab = Screen.HOME_SCREEN,
                            )
                        findNavController()
                            .navigate(R.id.action_smallPreviewFragment_to_fullPreviewFragment)
                    }
                ),
            lifecycleOwner = viewLifecycleOwner,
            offsetToStart = false,
            onWallpaperPreviewDirty = { activity?.recreate() },
        )
    }
}
