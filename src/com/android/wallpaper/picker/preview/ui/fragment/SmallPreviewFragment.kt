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
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.preview.di.modules.preview.utils.PreviewUtilsModule
import com.android.wallpaper.picker.preview.ui.binder.DualPreviewSelectorBinder
import com.android.wallpaper.picker.preview.ui.binder.PreviewSelectorBinder
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.DualPreviewViewPager
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.adapters.DualPreviewPagerAdapter
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.RtlUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * This fragment displays the preview of the selected wallpaper on all available workspaces and
 * device displays.
 */
@AndroidEntryPoint(AppbarFragment::class)
class SmallPreviewFragment : Hilt_SmallPreviewFragment() {

    @Inject lateinit var displayUtils: DisplayUtils
    @PreviewUtilsModule.HomeScreenPreviewUtils @Inject lateinit var homePreviewUtils: PreviewUtils
    @PreviewUtilsModule.LockScreenPreviewUtils @Inject lateinit var lockPreviewUtils: PreviewUtils
    @Inject @MainDispatcher lateinit var mainScope: CoroutineScope

    private val wallpaperPreviewViewModel by activityViewModels<WallpaperPreviewViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view =
            if (displayUtils.hasMultiInternalDisplays()) {
                inflater.inflate(R.layout.fragment_small_preview_for_two_screens, container, false)
            } else {
                inflater.inflate(R.layout.fragment_small_preview_handheld, container, false)
            }
        setUpToolbar(view)
        bindScreenPreview(view)

        return view
    }

    override fun getDefaultTitle(): CharSequence {
        return getString(R.string.preview)
    }

    override fun getToolbarColorId(): Int {
        return android.R.color.transparent
    }

    override fun getToolbarTextColor(): Int {
        return ContextCompat.getColor(requireContext(), R.color.system_on_surface)
    }

    private fun bindScreenPreview(view: View) {
        val activity = activity ?: return
        val applicationContext = activity.applicationContext
        val isSingleDisplayOrUnfoldedHorizontalHinge =
            displayUtils.isSingleDisplayOrUnfoldedHorizontalHinge(activity)
        val isRtl = RtlUtils.isRtl(applicationContext)

        if (displayUtils.hasMultiInternalDisplays()) {
            val dualPreviewView: DualPreviewViewPager =
                view.requireViewById(R.id.dual_preview_pager)
            DualPreviewSelectorBinder.bind(
                dualPreviewView,
                DualPreviewPagerAdapter.DualPreviewPagerViewModel(
                    wallpaperPreviewViewModel,
                    homePreviewUtils
                ) {
                    findNavController()
                        .navigate(R.id.action_smallPreviewFragment_to_fullPreviewFragment)
                },
                DualPreviewPagerAdapter.DualPreviewPagerViewModel(
                    wallpaperPreviewViewModel,
                    lockPreviewUtils
                ) {
                    findNavController()
                        .navigate(R.id.action_smallPreviewFragment_to_fullPreviewFragment)
                },
                applicationContext,
                isSingleDisplayOrUnfoldedHorizontalHinge,
                viewLifecycleOwner,
                isRtl,
                mainScope,
                displayUtils,
            )
        } else {
            PreviewSelectorBinder.bind(
                tabsViewPager = view.requireViewById(R.id.pager_tabs),
                previewsViewPager = view.requireViewById(R.id.pager_previews),
                previewDisplaySize = displayUtils.getRealSize(displayUtils.getWallpaperDisplay()),
                // TODO: pass correct view models for the view pager
                wallpaperPreviewViewModels =
                    listOf(wallpaperPreviewViewModel, wallpaperPreviewViewModel),
                applicationContext = applicationContext,
                isSingleDisplayOrUnfoldedHorizontalHinge = isSingleDisplayOrUnfoldedHorizontalHinge,
                viewLifecycleOwner = viewLifecycleOwner,
                isRtl = isRtl,
                mainScope = mainScope,
                previewUtils = homePreviewUtils,
                navigate = {
                    findNavController()
                        .navigate(R.id.action_smallPreviewFragment_to_fullPreviewFragment)
                },
            )
        }
    }
}
