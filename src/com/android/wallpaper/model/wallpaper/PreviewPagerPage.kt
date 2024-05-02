package com.android.wallpaper.model.wallpaper

import com.android.wallpaper.module.CustomizationSections.Screen

/**
 * Pages on the small preview pager and their page position as ordinal.
 *
 * Indicates which of the HS/LS preview overlay is selected, but not HS/LS wallpaper, for which
 * wallpaper is selected, see [isViewAsHome] passsed to [WallpaperPreviewActivity].
 */
enum class PreviewPagerPage(val screen: Screen) {
    LOCK_PREVIEW(Screen.LOCK_SCREEN),
    HOME_PREVIEW(Screen.HOME_SCREEN),
}
