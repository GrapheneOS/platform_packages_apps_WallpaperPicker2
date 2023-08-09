package com.android.wallpaper.picker.preview.ui.util

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import kotlin.math.abs

/** Util for calculating cropping related size. */
object CropSizeUtil {

    /**
     * Finds largest possible rectangle in [Rect] with the same aspect ratio in the target
     * rectangle.
     *
     * See [findMaxRectWithRatioIn].
     */
    fun Rect.findMaxRectWithRatioIn(rect: Rect): PointF =
        Point(width(), height()).findMaxRectWithRatioIn(Point(rect.width(), rect.height()))

    /**
     * Finds largest possible rectangle in [Point] with the same aspect ratio in the target
     * rectangle.
     *
     * Calling rectangle will scale up or down to match the size of the target rectangle, the final
     * rectangle size is the largest but not exceeding the size of target rectangle.
     */
    fun Point.findMaxRectWithRatioIn(point: Point): PointF {
        val ratio =
            if (x <= point.x && y <= point.y) {
                // Target rect is containing the calling rect
                if (abs(x - point.x) <= abs(y - point.y)) {
                    point.x.toFloat() / x
                } else {
                    point.y.toFloat() / y
                }
            } else if (x > point.x && y > point.y) {
                // Calling rect is containing the target rect
                if (abs(x - point.x) >= abs(y - point.y)) {
                    point.x.toFloat() / x
                } else {
                    point.y.toFloat() / y
                }
            } else {
                // Target rect and calling rect overlap
                if (x > point.x) {
                    point.x.toFloat() / x
                } else {
                    point.y.toFloat() / y
                }
            }

        return PointF(x * ratio, y * ratio)
    }
}
