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
        val view =
            inflater.inflate(R.layout.fragment_small_preview, container, /* attachToRoot= */ false)
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
        ScreenPreviewBinder.bind(
            activity = requireActivity(),
            previewView = view.requireViewById(R.id.preview),
            viewModel =
                ScreenPreviewViewModel(
                    previewUtils =
                        PreviewUtils(
                            context = requireContext(),
                            authorityMetadataKey =
                                requireContext()
                                    .getString(
                                        R.string.grid_control_metadata_name,
                                    ),
                        ),
                    wallpaperInfoProvider = { wallpaperPreviewViewModel.editingWallpaper },
                    wallpaperInteractor =
                        InjectorProvider.getInjector().getWallpaperInteractor(requireContext()),
                    screen = CustomizationSections.Screen.HOME_SCREEN,
                    onPreviewClicked = {
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
