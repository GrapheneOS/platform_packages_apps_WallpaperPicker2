package com.android.wallpaper.picker.preview.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Top level [ViewModel] for [WallpaperPreviewActivity] and its fragments */
@HiltViewModel
class WallpaperPreviewViewModel @Inject constructor() : ViewModel() {
    /** User selected [WallpaperInfo] for editing. */
    var editingWallpaper: WallpaperInfo? = null
}
