/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.wallpaper.util;

import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.Display;

/**
 * Static utility methods for wallpaper cropping operations.
 */
public final class WallpaperCropUtils {

    private static final float WALLPAPER_SCREENS_SPAN = 2f;
    private static final float ASPECT_RATIO_LANDSCAPE = 16 / 10f;
    private static final float ASPECT_RATIO_PORTRAIT = 10 / 16f;
    private static final float WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE = 1.2f;
    private static final float WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT = 1.5f;

    // Suppress default constructor for noninstantiability.
    private WallpaperCropUtils() {
        throw new AssertionError();
    }

    /**
     * Calculates parallax travel (i.e., extra width) for a screen with the given resolution.
     */
    public static float wallpaperTravelToScreenWidthRatio(int width, int height) {
        float aspectRatio = width / (float) height;

        // At an aspect ratio of 16/10, the wallpaper parallax effect should span 1.2 * screen width
        // At an aspect ratio of 10/16, the wallpaper parallax effect should span 1.5 * screen width
        // We will use these two data points to extrapolate how much the wallpaper parallax effect
        // to span (i.e., travel) at any aspect ratio. We use the following two linear formulas, where
        // the coefficient on x is the aspect ratio (width/height):
        //   (16/10)x + y = 1.2
        //   (10/16)x + y = 1.5
        // We solve for x and y and end up with a final formula:
        float x = (WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE - WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT)
                / (ASPECT_RATIO_LANDSCAPE - ASPECT_RATIO_PORTRAIT);
        float y = WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT - x * ASPECT_RATIO_PORTRAIT;
        return x * aspectRatio + y;
    }

    /**
     * Calculates ideal crop surface size for a device such that there is room for parallax in both
     * landscape and portrait screen orientations.
     */
    public static Point getDefaultCropSurfaceSize(Resources resources, Display display) {
        Point minDims = new Point();
        Point maxDims = new Point();
        display.getCurrentSizeRange(minDims, maxDims);

        int maxDim = Math.max(maxDims.x, maxDims.y);
        int minDim = Math.max(minDims.x, minDims.y);

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            Point realSize = new Point();
            display.getRealSize(realSize);
            maxDim = Math.max(realSize.x, realSize.y);
            minDim = Math.min(realSize.x, realSize.y);
        }

        final int defaultWidth, defaultHeight;
        if (resources.getConfiguration().smallestScreenWidthDp >= 720) {
            defaultWidth = (int) (maxDim * wallpaperTravelToScreenWidthRatio(maxDim, minDim));
            defaultHeight = maxDim;
        } else {
            defaultWidth = Math.max((int) (minDim * WALLPAPER_SCREENS_SPAN), maxDim);
            defaultHeight = maxDim;
        }

        return new Point(defaultWidth, defaultHeight);
    }

    /**
     * Calculates the relative position between an outer and inner rectangle when the outer one is
     * center-cropped to the inner one.
     *
     * @param outer      Size of outer rectangle as a Point (x,y).
     * @param inner      Size of inner rectangle as a Point (x,y).
     * @param alignStart Whether the inner rectangle should be aligned to the start of the layout with
     *                   the outer one and ignore horizontal centering.
     * @param isRtl      Whether the layout direction is RTL (or false for LTR).
     * @return Position relative to the top left corner of the outer rectangle, where the size of each
     * rectangle is represented by Points, in coordinates (x,y) relative to the outer rectangle
     * where the top left corner is (0,0)
     * @throws IllegalArgumentException if inner rectangle is not contained within outer rectangle
     *                                  which would return a position with at least one negative coordinate.
     */
    public static Point calculateCenterPosition(Point outer, Point inner, boolean alignStart,
                                                boolean isRtl) {
        if (inner.x > outer.x || inner.y > outer.y) {
            throw new IllegalArgumentException("Inner rectangle " + inner + " should be contained"
                    + " completely within the outer rectangle " + outer + ".");
        }

        Point relativePosition = new Point();

        if (alignStart) {
            relativePosition.x = isRtl ? outer.x - inner.x : 0;
        } else {
            relativePosition.x = Math.round((outer.x - inner.x) / 2f);
        }
        relativePosition.y = Math.round((outer.y - inner.y) / 2f);

        return relativePosition;
    }

    /**
     * Calculates the minimum zoom such that the maximum surface area of the outer rectangle is
     * visible within the inner rectangle.
     *
     * @param outer Size of outer rectangle as a Point (x,y).
     * @param inner Size of inner rectangle as a Point (x,y).
     */
    public static float calculateMinZoom(Point outer, Point inner) {
        float minZoom;
        if (inner.x / (float) inner.y > outer.x / (float) outer.y) {
            minZoom = inner.x / (float) outer.x;
        } else {
            minZoom = inner.y / (float) outer.y;
        }
        return minZoom;
    }

    /**
     * Calculates the center position of a wallpaper of the given size, based on a "crop surface"
     * (with extra width to account for parallax) superimposed on the screen. Trying to show as
     * much of the wallpaper as possible on the crop surface and align screen to crop surface such
     * that the centered wallpaper matches what would be seen by the user in the left-most home
     * screen.
     *
     * @param wallpaperSize full size of the wallpaper
     * @param cropSurfaceSize ideal crop size
     *          @see #getDefaultCropSurfaceSize(Resources, Display)
     * @param screenSize size of the display
     * @param defaultWallpaperScale default scale the wallpaper is set to
     * @param isRtl whether we're in RTL mode
     * @return a point corresponding to the position of wallpaper that should be in the center
     *      of the screen.
     */
    public static PointF calculateDefaultCenter(Point wallpaperSize, Point cropSurfaceSize,
            Point screenSize, float defaultWallpaperScale, boolean isRtl) {
        Point screenToCropSurfacePosition = calculateCenterPosition(
                cropSurfaceSize, screenSize, true /* alignStart */, isRtl);
        Point zoomedWallpaperSize = new Point(
                Math.round(wallpaperSize.x * defaultWallpaperScale),
                Math.round(wallpaperSize.y * defaultWallpaperScale));
        Point cropSurfaceToWallpaperPosition = calculateCenterPosition(
                zoomedWallpaperSize, cropSurfaceSize, false /* alignStart */, isRtl);

        // Set center to composite positioning between scaled wallpaper and screen.
        PointF centerPosition = new PointF(
                wallpaperSize.x / 2f,
                wallpaperSize.y / 2f);
        centerPosition.offset(-(screenToCropSurfacePosition.x + cropSurfaceToWallpaperPosition.x),
                -(screenToCropSurfacePosition.y + cropSurfaceToWallpaperPosition.y));
        return centerPosition;
    }

    /**
     * Calculates the rectangle to crop a wallpaper of the given size, and considering the given
     * scrollX and scrollY offsets
     * @param wallpaperZoom zoom applied to the raw wallpaper image
     * @param wallpaperSize full ("raw") wallpaper size
     * @param defaultCropSurfaceSize @see #getDefaultCropSurfaceSize(Resources, Display)
     * @param screenSize size of the display
     * @param scrollX x-axis offset the cropping area from the wallpaper's 0,0 position
     * @param scrollY y-axis offset the cropping area from the wallpaper's 0,0 position
     * @param isRtl whether we're in RTL mode
     * @return a Rect representing the area of the wallpaper to crop.
     */
    public static Rect calculateCropRect(float wallpaperZoom, Point wallpaperSize,
            Point defaultCropSurfaceSize, Point screenSize, int scrollX, int scrollY,
            boolean isRtl) {
        // Calculate Rect of wallpaper in physical pixel terms (i.e., scaled to current zoom).
        int scaledWallpaperWidth = (int) (wallpaperSize.x * wallpaperZoom);
        int scaledWallpaperHeight = (int) (wallpaperSize.y * wallpaperZoom);
        Rect rect = new Rect();
        rect.set(0, 0, scaledWallpaperWidth, scaledWallpaperHeight);

        // Crop rect should start off as the visible screen and then include extra width and height
        // if available within wallpaper at the current zoom.
        Rect cropRect = new Rect(scrollX, scrollY, scrollX + screenSize.x, scrollY + screenSize.y);

        int extraWidth = defaultCropSurfaceSize.x - screenSize.x;
        int extraHeightTopAndBottom = (int) ((defaultCropSurfaceSize.y - screenSize.y) / 2f);

        // Try to increase size of screenRect to include extra width depending on the layout
        // direction.
        if (isRtl) {
            cropRect.left = Math.max(cropRect.left - extraWidth, rect.left);
        } else {
            cropRect.right = Math.min(cropRect.right + extraWidth, rect.right);
        }

        // Try to increase the size of the cropRect to to include extra height.
        int availableExtraHeightTop = cropRect.top - Math.max(
                rect.top,
                cropRect.top - extraHeightTopAndBottom);
        int availableExtraHeightBottom = Math.min(
                rect.bottom,
                cropRect.bottom + extraHeightTopAndBottom) - cropRect.bottom;

        int availableExtraHeightTopAndBottom =
                Math.min(availableExtraHeightTop, availableExtraHeightBottom);
        cropRect.top -= availableExtraHeightTopAndBottom;
        cropRect.bottom += availableExtraHeightTopAndBottom;

        return cropRect;
    }
}
