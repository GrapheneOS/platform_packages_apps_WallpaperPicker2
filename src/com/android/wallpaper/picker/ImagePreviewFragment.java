/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.wallpaper.picker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.wallpaper.R;
import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.module.WallpaperPersister.Destination;
import com.android.wallpaper.module.WallpaperPersister.SetWallpaperCallback;
import com.android.wallpaper.util.ScreenSizeCalculator;
import com.android.wallpaper.util.WallpaperCropUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

/**
 * Fragment which displays the UI for previewing an individual static wallpaper and its attribution
 * information.
 */
public class ImagePreviewFragment extends PreviewFragment {

    private static final float DEFAULT_WALLPAPER_MAX_ZOOM = 8f;

    private SubsamplingScaleImageView mFullResImageView;
    private Asset mWallpaperAsset;
    private Point mDefaultCropSurfaceSize;
    private Point mScreenSize;
    private Point mRawWallpaperSize; // Native size of wallpaper image.
    private ImageView mLowResImageView;

    private InfoPageController mInfoPageController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWallpaperAsset = mWallpaper.getAsset(requireContext().getApplicationContext());
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_image_preview;
    }


    protected int getBottomSheetResId() {
        return R.id.bottom_sheet;
    }

    @Override
    protected int getLoadingIndicatorResId() {
        return R.id.loading_indicator;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Activity activity = requireActivity();

        mFullResImageView = view.findViewById(R.id.full_res_image);

        mInfoPageController = new InfoPageController(view.findViewById(R.id.page_info),
                mPreviewMode);

        mLowResImageView = view.findViewById(R.id.low_res_image);

        // Trim some memory from Glide to make room for the full-size image in this fragment.
        Glide.get(activity).setMemoryCategory(MemoryCategory.LOW);

        mDefaultCropSurfaceSize = WallpaperCropUtils.getDefaultCropSurfaceSize(
                getResources(), activity.getWindowManager().getDefaultDisplay());
        mScreenSize = ScreenSizeCalculator.getInstance().getScreenSize(
                activity.getWindowManager().getDefaultDisplay());

        // Load a low-res placeholder image if there's a thumbnail available from the asset that can
        // be shown to the user more quickly than the full-sized image.
        if (mWallpaperAsset.hasLowResDataSource()) {
            mWallpaperAsset.loadLowResDrawable(activity, mLowResImageView, Color.BLACK,
                    new WallpaperPreviewBitmapTransformation(activity.getApplicationContext(),
                            isRtl()));
        }

        mWallpaperAsset.decodeRawDimensions(getActivity(), dimensions -> {
            // Don't continue loading the wallpaper if the Fragment is detached.
            if (getActivity() == null) {
                return;
            }

            // Return early and show a dialog if dimensions are null (signaling a decoding error).
            if (dimensions == null) {
                showLoadWallpaperErrorDialog();
                return;
            }

            mRawWallpaperSize = dimensions;
            setUpExploreIntent(ImagePreviewFragment.this::initFullResView);
        });

        setUpLoadingIndicator();

        return view;
    }

    @Override
    protected void setUpBottomSheetView(ViewGroup bottomSheet) {
        // Nothing needed here.
    }

    @Override
    protected boolean isLoaded() {
        return mFullResImageView != null && mFullResImageView.hasImage();
    }

    @Override
    public void onClickOk() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLoadingProgressBar != null) {
            mLoadingProgressBar.hide();
        }
        mFullResImageView.recycle();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
        outState.putInt(KEY_BOTTOM_SHEET_STATE, bottomSheetBehavior.getState());
    }

    @Override
    protected void setBottomSheetContentAlpha(float alpha) {
        mInfoPageController.setContentAlpha(alpha);
    }

    @Override
    protected CharSequence getExploreButtonLabel(Context context) {
        return context.getString(mWallpaper.getActionLabelRes(context));
    }

    /**
     * Initializes MosaicView by initializing tiling, setting a fallback page bitmap, and
     * initializing a zoom-scroll observer and click listener.
     */
    private void initFullResView() {
        mFullResImageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);

        // Set a solid black "page bitmap" so MosaicView draws a black background while waiting
        // for the image to load or a transparent one if a thumbnail already loaded.
        Bitmap blackBitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
        int color = (mLowResImageView.getDrawable() == null) ? Color.BLACK : Color.TRANSPARENT;
        blackBitmap.setPixel(0, 0, color);
        mFullResImageView.setImage(ImageSource.bitmap(blackBitmap));

        // Then set a fallback "page bitmap" to cover the whole MosaicView, which is an actual
        // (lower res) version of the image to be displayed.
        Point targetPageBitmapSize = new Point(mRawWallpaperSize);
        mWallpaperAsset.decodeBitmap(targetPageBitmapSize.x, targetPageBitmapSize.y,
                pageBitmap -> {
                    // Check that the activity is still around since the decoding task started.
                    if (getActivity() == null) {
                        return;
                    }

                    // Some of these may be null depending on if the Fragment is paused, stopped,
                    // or destroyed.
                    if (mLoadingProgressBar != null) {
                        mLoadingProgressBar.hide();
                    }
                    // The page bitmap may be null if there was a decoding error, so show an
                    // error dialog.
                    if (pageBitmap == null) {
                        showLoadWallpaperErrorDialog();
                        return;
                    }
                    if (mFullResImageView != null) {
                        // Set page bitmap.
                        mFullResImageView.setImage(ImageSource.bitmap(pageBitmap));

                        setDefaultWallpaperZoomAndScroll();
                        crossFadeInMosaicView();
                    }
                    getActivity().invalidateOptionsMenu();

                    populateInfoPage(mInfoPageController);
                });
    }

    /**
     * Makes the MosaicView visible with an alpha fade-in animation while fading out the loading
     * indicator.
     */
    private void crossFadeInMosaicView() {
        long shortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        mFullResImageView.setAlpha(0f);
        mFullResImageView.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Clear the thumbnail bitmap reference to save memory since it's no longer
                        // visible.
                        if (mLowResImageView != null) {
                            mLowResImageView.setImageBitmap(null);
                        }
                    }
                });

        mLoadingProgressBar.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mLoadingProgressBar != null) {
                            mLoadingProgressBar.hide();
                        }
                    }
                });
    }

    /**
     * Sets the default wallpaper zoom and scroll position based on a "crop surface"
     * (with extra width to account for parallax) superimposed on the screen. Shows as much of the
     * wallpaper as possible on the crop surface and align screen to crop surface such that the
     * default preview matches what would be seen by the user in the left-most home screen.
     *
     * <p>This method is called once in the Fragment lifecycle after the wallpaper asset has loaded
     * and rendered to the layout.
     */
    private void setDefaultWallpaperZoomAndScroll() {
        // Determine minimum zoom to fit maximum visible area of wallpaper on crop surface.
        float defaultWallpaperZoom =
                WallpaperCropUtils.calculateMinZoom(mRawWallpaperSize, mDefaultCropSurfaceSize);
        float minWallpaperZoom =
                WallpaperCropUtils.calculateMinZoom(mRawWallpaperSize, mScreenSize);

        Point screenToCropSurfacePosition = WallpaperCropUtils.calculateCenterPosition(
                mDefaultCropSurfaceSize, mScreenSize, true /* alignStart */, isRtl());
        Point zoomedWallpaperSize = new Point(
                Math.round(mRawWallpaperSize.x * defaultWallpaperZoom),
                Math.round(mRawWallpaperSize.y * defaultWallpaperZoom));
        Point cropSurfaceToWallpaperPosition = WallpaperCropUtils.calculateCenterPosition(
                zoomedWallpaperSize, mDefaultCropSurfaceSize, false /* alignStart */, isRtl());

        // Set min wallpaper zoom and max zoom on MosaicView widget.
        mFullResImageView.setMaxScale(Math.max(DEFAULT_WALLPAPER_MAX_ZOOM, defaultWallpaperZoom));
        mFullResImageView.setMinScale(minWallpaperZoom);

        // Set center to composite positioning between scaled wallpaper and screen.
        PointF centerPosition = new PointF(
                mRawWallpaperSize.x / 2f,
                mRawWallpaperSize.y / 2f);
        centerPosition.offset(-(screenToCropSurfacePosition.x + cropSurfaceToWallpaperPosition.x),
                -(screenToCropSurfacePosition.y + cropSurfaceToWallpaperPosition.y));

        mFullResImageView.setScaleAndCenter(minWallpaperZoom, centerPosition);
    }

    private Rect calculateCropRect() {
        // Calculate Rect of wallpaper in physical pixel terms (i.e., scaled to current zoom).
        float wallpaperZoom = mFullResImageView.getScale();
        int scaledWallpaperWidth = (int) (mRawWallpaperSize.x * wallpaperZoom);
        int scaledWallpaperHeight = (int) (mRawWallpaperSize.y * wallpaperZoom);
        Rect rect = new Rect();
        mFullResImageView.visibleFileRect(rect);
        int scrollX = (int) (rect.left * wallpaperZoom);
        int scrollY = (int) (rect.top * wallpaperZoom);

        rect.set(0, 0, scaledWallpaperWidth, scaledWallpaperHeight);

        Display defaultDisplay =  requireActivity().getWindowManager().getDefaultDisplay();
        Point screenSize = ScreenSizeCalculator.getInstance().getScreenSize(defaultDisplay);
        // Crop rect should start off as the visible screen and then include extra width and height
        // if available within wallpaper at the current zoom.
        Rect cropRect = new Rect(scrollX, scrollY, scrollX + screenSize.x, scrollY + screenSize.y);

        Point defaultCropSurfaceSize = WallpaperCropUtils.getDefaultCropSurfaceSize(
                getResources(), defaultDisplay);
        int extraWidth = defaultCropSurfaceSize.x - screenSize.x;
        int extraHeightTopAndBottom = (int) ((defaultCropSurfaceSize.y - screenSize.y) / 2f);

        // Try to increase size of screenRect to include extra width depending on the layout
        // direction.
        if (isRtl()) {
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

    @Override
    protected void setCurrentWallpaper(@Destination int destination) {
        mWallpaperSetter.setCurrentWallpaper(getActivity(), mWallpaper, mWallpaperAsset,
                destination, mFullResImageView.getScale(), calculateCropRect(),
                new SetWallpaperCallback() {
                    @Override
                    public void onSuccess() {
                        finishActivityWithResultOk();
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable) {
                        showSetWallpaperErrorDialog(destination);
                    }
                });
    }
}
