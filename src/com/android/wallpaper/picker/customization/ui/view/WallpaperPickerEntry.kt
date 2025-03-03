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

package com.android.wallpaper.picker.customization.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R

/**
 * The wallpaper entry on the main screen. This view needs to be in a [ConstraintLayout].
 *
 * This view is used to display the wallpaper option in the customization screen. It has a collapsed
 * state and an expanded state. The collapsed state shows a button that allows the user to select a
 * wallpaper. The expanded state shows a carousel of wallpapers that are related to the selected
 * wallpaper.
 */
class WallpaperPickerEntry
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    val collapsedButton: TextView
    val moreWallpapersButton: TextView
    val suggestedPhotosText: TextView
    val wallpaperCarousel: RecyclerView

    private val backgroundLayout: FrameLayout
    val background: GradientDrawable
    private val expandedContainer: ConstraintLayout

    private val defaultCornerRadius: Float
    private val expandedBackgroundTopCornerRadius: Float

    private var expandedWidth = 0
    private var expandedHeight = 0
    private var collapsedHeight = 0
    private var collapsedWidth = 0

    init {
        inflate(context, R.layout.wallpaper_picker_entry, this)

        collapsedButton =
            requireViewById(R.id.customization_option_entry_wallpaper_collapsed_button)
        moreWallpapersButton = requireViewById(R.id.more_wallpapers_button)
        suggestedPhotosText = requireViewById(R.id.wallpaper_picker_entry_title)
        wallpaperCarousel = requireViewById(R.id.wallpaper_carousel)

        backgroundLayout = requireViewById(R.id.wallpaper_picker_entry_background)
        background = backgroundLayout.background as GradientDrawable
        expandedContainer = requireViewById(R.id.wallpaper_picker_entry_expanded_container)

        defaultCornerRadius =
            resources
                .getDimensionPixelSize(R.dimen.customization_option_entry_corner_radius_large)
                .toFloat()
        expandedBackgroundTopCornerRadius =
            resources
                .getDimensionPixelSize(R.dimen.wallpaper_picker_entry_background_top_corner_radius)
                .toFloat()

        clipChildren = false
        clipToPadding = false

        post {
            // Make fixed width and height of the container, so it does not shrink with parent.
            expandedContainer.layoutParams =
                LayoutParams(expandedContainer.width, expandedContainer.height).apply {
                    gravity = Gravity.CENTER
                }

            expandedWidth = width
            expandedHeight = height
            collapsedWidth =
                collapsedButton.width +
                    resources.getDimensionPixelSize(
                        R.dimen.customization_option_container_horizontal_padding
                    ) * 4
            collapsedHeight = collapsedButton.height
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        val clipPath = Path()

        clipPath.addRoundRect(
            paddingStart.toFloat(),
            0f,
            paddingStart.toFloat() + backgroundLayout.width.toFloat(),
            backgroundLayout.height.toFloat(),
            background.cornerRadii ?: FloatArray(8),
            Path.Direction.CW,
        )
        canvas.clipPath(clipPath)

        super.dispatchDraw(canvas)
    }

    /**
     * Set collapsing progress of the [WallpaperPickerEntry]
     *
     * @param progress 0.0 means fully expanded and 1.0 means fully collapsed
     */
    fun setProgress(progress: Float) {
        collapsedButton.alpha = progress
        expandedContainer.alpha = 1 - progress
        val radii = background.cornerRadii ?: FloatArray(8)
        val topCornerRadius =
            expandedBackgroundTopCornerRadius -
                (expandedBackgroundTopCornerRadius - defaultCornerRadius) * progress
        radii[0] = topCornerRadius
        radii[1] = topCornerRadius
        radii[2] = topCornerRadius
        radii[3] = topCornerRadius
        radii[4] = defaultCornerRadius
        radii[5] = defaultCornerRadius
        radii[6] = defaultCornerRadius
        radii[7] = defaultCornerRadius
        background.cornerRadii = radii

        val params = layoutParams as ConstraintLayout.LayoutParams

        params.width = (expandedWidth - (expandedWidth - collapsedWidth) * progress).toInt()
        params.height = (expandedHeight - (expandedHeight - collapsedHeight) * progress).toInt()
        layoutParams = params
    }
}
