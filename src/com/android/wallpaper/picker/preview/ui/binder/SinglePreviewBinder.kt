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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.android.wallpaper.R
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.adapters.DualPreviewPagerAdapter
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.adapters.SinglePreviewPagerAdapter
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.adapters.TabTextPagerAdapter
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.pagetransformers.PreviewCardPageTransformer
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.pagetransformers.PreviewTabsPageTransformer
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.CoroutineScope

/** This class initializes and synchronizes the tab and preview view pagers */
object SinglePreviewBinder {

    fun bind(
        tabsViewPager: ViewPager,
        previewsViewPager: ViewPager2,
        previewDisplaySize: Point,
        wallpaperPreviewViewModels: List<WallpaperPreviewViewModel>,
        applicationContext: Context,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        viewLifecycleOwner: LifecycleOwner,
        isRtl: Boolean,
        mainScope: CoroutineScope,
        navigate: (() -> Unit)? = null,
    ) {
        // set up tabs view pager
        bindTabsViewPager(tabsViewPager, previewDisplaySize)

        // set up previews view pager
        bindPreviewsViewPager(
            applicationContext,
            isSingleDisplayOrUnfoldedHorizontalHinge,
            viewLifecycleOwner,
            isRtl,
            mainScope,
            previewsViewPager,
            wallpaperPreviewViewModels,
            previewDisplaySize,
            navigate,
        )

        // synchronize the two pagers
        synchronizePreviewAndTabsPager(tabsViewPager, previewsViewPager)
    }

    @SuppressLint("WrongConstant")
    private fun bindPreviewsViewPager(
        applicationContext: Context,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        viewLifecycleOwner: LifecycleOwner,
        isRtl: Boolean,
        mainScope: CoroutineScope,
        previewsViewPager: ViewPager2,
        wallpaperPreviewViewModels: List<WallpaperPreviewViewModel>,
        previewDisplaySize: Point,
        navigate: (() -> Unit)? = null,
    ) {
        previewsViewPager.apply {
            adapter = SinglePreviewPagerAdapter { viewHolder, position ->
                SmallPreviewBinder.bind(
                    applicationContext = applicationContext,
                    view = viewHolder.itemView.requireViewById(R.id.preview),
                    viewModel = wallpaperPreviewViewModels[position],
                    mainScope = mainScope,
                    viewLifecycleOwner = viewLifecycleOwner,
                    isSingleDisplayOrUnfoldedHorizontalHinge =
                        isSingleDisplayOrUnfoldedHorizontalHinge,
                    isRtl = isRtl,
                    previewDisplaySize = previewDisplaySize,
                    navigate = navigate,
                )
            }
            offscreenPageLimit = DualPreviewPagerAdapter.PREVIEW_PAGER_ITEM_COUNT
            clipChildren = false
            clipToPadding = false
            setPageTransformer(PreviewCardPageTransformer(previewDisplaySize))
        }
    }

    private fun bindTabsViewPager(
        tabsViewPager: ViewPager,
        previewDisplaySize: Point,
    ) {
        tabsViewPager.apply {
            adapter = TabTextPagerAdapter()
            offscreenPageLimit = 2
            clipChildren = false
            clipToPadding = false
            setPageTransformer(
                /* reverseDrawingOrder= */ true,
                PreviewTabsPageTransformer(previewDisplaySize)
            )
        }
    }

    private fun synchronizePreviewAndTabsPager(
        tabsViewPager: ViewPager,
        previewsViewPager: ViewPager2,
    ) {
        val onPageChangeListener =
            object : ViewPager.OnPageChangeListener {
                override fun onPageSelected(position: Int) {
                    previewsViewPager.setCurrentItem(position, true)
                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {}

                override fun onPageScrollStateChanged(state: Int) {}
            }

        tabsViewPager.addOnPageChangeListener(onPageChangeListener)

        previewsViewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    tabsViewPager.setCurrentItem(position, true)
                }
            }
        )
    }
}
