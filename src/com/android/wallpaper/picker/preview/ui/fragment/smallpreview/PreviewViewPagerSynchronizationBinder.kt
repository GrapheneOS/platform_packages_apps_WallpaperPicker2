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
package com.android.wallpaper.picker.preview.ui.fragment.smallpreview

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.adapters.SinglePreviewPagerAdapter
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.adapters.TabTextPagerAdapter
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.pagetransformers.PreviewCardPageTransformer
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.pagetransformers.PreviewTabsPageTransformer
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.CoroutineScope

/** This class initializes and synchronizes the tab and preview view pagers */
object PreviewViewPagerSynchronizationBinder {

    fun bind(
        tabsViewPager: ViewPager,
        previewsViewPager: ViewPager2,
        screenSizePx: Point,
        resources: Resources,
        screenPreviewViewModels: List<WallpaperPreviewViewModel>,
        applicationContext: Context,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        lifecycleOwner: LifecycleOwner,
        isRtl: Boolean,
        mainScope: CoroutineScope,
    ) {
        // set up tabs view pager
        bindTabsViewPager(tabsViewPager, resources, screenSizePx)

        // set up previews view pager
        bindPreviewsViewPager(
            applicationContext,
            isSingleDisplayOrUnfoldedHorizontalHinge,
            lifecycleOwner,
            isRtl,
            mainScope,
            previewsViewPager,
            resources,
            screenPreviewViewModels,
            screenSizePx
        )

        // synchronize the two pagers
        synchronizePreviewAndTabsPager(tabsViewPager, previewsViewPager)
    }

    private fun bindPreviewsViewPager(
        applicationContext: Context,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        lifecycleOwner: LifecycleOwner,
        isRtl: Boolean,
        mainScope: CoroutineScope,
        previewsViewPager: ViewPager2,
        resources: Resources,
        screenPreviewViewModels: List<WallpaperPreviewViewModel>,
        screenSizePx: Point,
    ) {
        previewsViewPager.adapter =
            SinglePreviewPagerAdapter(
                applicationContext,
                isSingleDisplayOrUnfoldedHorizontalHinge,
                lifecycleOwner,
                isRtl,
                mainScope,
                screenPreviewViewModels
            )
        previewsViewPager.offscreenPageLimit = 2
        previewsViewPager.clipChildren = false
        previewsViewPager.clipToPadding = false
        previewsViewPager.setPageTransformer(PreviewCardPageTransformer(screenSizePx, resources))
    }

    private fun bindTabsViewPager(
        tabsViewPager: ViewPager,
        resources: Resources,
        screenSizePx: Point
    ) {
        tabsViewPager.adapter = TabTextPagerAdapter(resources)
        tabsViewPager.offscreenPageLimit = 2
        tabsViewPager.clipChildren = false
        tabsViewPager.clipToPadding = false
        tabsViewPager.setPageTransformer(true, PreviewTabsPageTransformer(screenSizePx))
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
