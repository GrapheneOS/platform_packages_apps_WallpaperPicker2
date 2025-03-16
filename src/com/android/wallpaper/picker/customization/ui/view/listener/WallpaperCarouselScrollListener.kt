/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.picker.customization.ui.view.listener

import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.picker.category.ui.view.adapter.CuratedPhotosAdapter
import com.google.android.material.carousel.CarouselLayoutManager

/**
 * This scroll listener implementation finds the positions of the first visible carousel item, and
 * updates the [CuratedPhotosAdapter] with that position
 */
class WallpaperCarouselScrollListener : RecyclerView.OnScrollListener() {
    private var lastScrollState = RecyclerView.NO_POSITION

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)

        if (
            newState == RecyclerView.SCROLL_STATE_IDLE &&
                lastScrollState != RecyclerView.SCROLL_STATE_IDLE
        ) {
            updateCarouselTitle(recyclerView)
        }
        lastScrollState = newState
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (lastScrollState == RecyclerView.NO_POSITION) {
            updateCarouselTitle(recyclerView)
        }
    }

    private fun updateCarouselTitle(recyclerView: RecyclerView) {
        // Get the LayoutManager
        val layoutManager = recyclerView.layoutManager as CarouselLayoutManager

        // Find the first completely visible item position
        var firstVisiblePosition = RecyclerView.NO_POSITION

        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i)
            if (child != null) {
                val position = recyclerView.getChildAdapterPosition(child)
                if (
                    position != RecyclerView.NO_POSITION &&
                        layoutManager.isViewPartiallyVisible(child, true, true)
                ) {
                    if (
                        firstVisiblePosition == RecyclerView.NO_POSITION ||
                            position < firstVisiblePosition
                    ) {
                        firstVisiblePosition = position
                    }
                }
            }
        }

        // Update the adapter with the new visible position
        if (firstVisiblePosition != RecyclerView.NO_POSITION) {
            recyclerView.post {
                (recyclerView.adapter as CuratedPhotosAdapter).setVisiblePosition(
                    firstVisiblePosition
                )
            }
        }
    }
}
