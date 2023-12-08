package com.android.wallpaper.picker.preview.ui.util

import android.graphics.Point
import android.graphics.PointF
import com.android.wallpaper.util.WallpaperCropUtils

object FullResImageViewUtil {

    private const val DEFAULT_WALLPAPER_MAX_ZOOM = 8f
    fun getScaleAndCenter(
        viewWidth: Int,
        viewHeight: Int,
        offsetToStart: Boolean,
        rawWallpaperSize: Point,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        isRtl: Boolean,
    ): ScaleAndCenter {
        // Determine minimum zoom to fit maximum visible area of wallpaper on crop surface.
        val crop = Point(viewWidth, viewHeight)
        val visibleRawWallpaperRect =
            WallpaperCropUtils.calculateVisibleRect(rawWallpaperSize, crop)
        if (offsetToStart && isSingleDisplayOrUnfoldedHorizontalHinge) {
            if (isRtl) {
                visibleRawWallpaperRect.offsetTo(
                    rawWallpaperSize.x - visibleRawWallpaperRect.width(),
                    visibleRawWallpaperRect.top,
                )
            } else {
                visibleRawWallpaperRect.offsetTo(/* newLeft= */ 0, visibleRawWallpaperRect.top)
            }
        }
        val centerPosition =
            PointF(
                visibleRawWallpaperRect.centerX().toFloat(),
                visibleRawWallpaperRect.centerY().toFloat()
            )
        val visibleRawWallpaperSize =
            Point(visibleRawWallpaperRect.width(), visibleRawWallpaperRect.height())
        val defaultWallpaperZoom =
            WallpaperCropUtils.calculateMinZoom(visibleRawWallpaperSize, crop)

        return ScaleAndCenter(
            defaultWallpaperZoom,
            defaultWallpaperZoom.coerceAtLeast(DEFAULT_WALLPAPER_MAX_ZOOM),
            defaultWallpaperZoom,
            centerPosition,
        )
    }

    data class ScaleAndCenter(
        val minScale: Float,
        val maxScale: Float,
        val defaultScale: Float,
        val center: PointF
    )
}
