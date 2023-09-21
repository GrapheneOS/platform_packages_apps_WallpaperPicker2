package com.android.wallpaper.picker.preview.ui.viewmodel

import android.graphics.Point
import com.android.wallpaper.picker.preview.ui.view.FullPreviewSurfaceView

/** View model for rendering [FullPreviewSurfaceView]. */
data class FullPreviewSurfaceViewModel(

    /** Data of user's selection on [SmallPreviewFragment]. */
    val previewTransitionViewModel: PreviewTransitionViewModel,

    /** Size of the current display rendering the view on screen. */
    val currentDisplaySize: Point,
)
