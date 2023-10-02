package com.android.wallpaper.picker.preview.ui.viewmodel

import android.graphics.Point
import com.android.wallpaper.module.CustomizationSections.Screen

/** Defines transitional data on [SmallPreviewFragment] when navigating to [FullPreviewFragment]. */
data class PreviewTransitionViewModel(

    /** [Screen] selected via preview tab to be rendered on next screen. */
    val previewTab: Screen,

    /** The display size the full preview is targeting on next screen. */
    val targetDisplaySize: Point,
)
