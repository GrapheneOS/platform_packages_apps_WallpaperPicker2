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
import androidx.fragment.app.activityViewModels
import com.android.wallpaper.R
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.CustomizationSections
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.customization.ui.binder.ScreenPreviewBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ScreenPreviewViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.PreviewUtils
import dagger.hilt.android.AndroidEntryPoint

/**
 * This fragment displays the preview of the selected wallpaper on all available workspaces and
 * device displays.
 */
@AndroidEntryPoint(AppbarFragment::class)
class SmallPreviewFragment : Hilt_SmallPreviewFragment() {

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_small_preview, container)

        val previewView: CardView = view.requireViewById(R.id.preview)

        val wallpaper: WallpaperInfo? = wallpaperPreviewViewModel.editingWallpaper

        ScreenPreviewBinder.bind(
            activity = requireActivity(),
            previewView = previewView,
            // TODO(b/295199906): view model injected in next diff
            viewModel =
                ScreenPreviewViewModel(
                    previewUtils =
                        PreviewUtils(
                            context = requireContext(),
                            authority =
                                requireContext()
                                    .getString(
                                        R.string.grid_control_metadata_name,
                                    ),
                        ),
                    wallpaperInfoProvider = { wallpaper },
                    // TODO(b/295199906): Interactor injected in next diff
                    wallpaperInteractor =
                        InjectorProvider.getInjector().getWallpaperInteractor(requireContext()),
                    screen = CustomizationSections.Screen.HOME_SCREEN,
                ),
            lifecycleOwner = viewLifecycleOwner,
            offsetToStart = false,
            onWallpaperPreviewDirty = { activity?.recreate() },
        )

        return view
    }
}
