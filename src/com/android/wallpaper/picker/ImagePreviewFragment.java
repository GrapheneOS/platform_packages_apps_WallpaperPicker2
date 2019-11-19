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

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

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
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.wallpaper.R;
import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.module.WallpaperPersister.Destination;
import com.android.wallpaper.module.WallpaperPersister.SetWallpaperCallback;
import com.android.wallpaper.util.ScreenSizeCalculator;
import com.android.wallpaper.util.WallpaperCropUtils;
import com.android.wallpaper.widget.MaterialProgressDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.List;

/**
 * Fragment which displays the UI for previewing an individual static wallpaper and its attribution
 * information.
 */
public class ImagePreviewFragment extends PreviewFragment {

    private static final float DEFAULT_WALLPAPER_MAX_ZOOM = 8f;

    private SubsamplingScaleImageView mFullResImageView;
    private Asset mWallpaperAsset;
    private TextView mAttributionTitle;
    private TextView mAttributionSubtitle1;
    private TextView mAttributionSubtitle2;
    private Button mExploreButton;
    private Button mSetWallpaperButton;

    private Point mDefaultCropSurfaceSize;
    private Point mScreenSize;
    private Point mRawWallpaperSize; // Native size of wallpaper image.
    private ImageView mLoadingIndicator;
    private MaterialProgressDrawable mProgressDrawable;
    private ImageView mLowResImageView;
    private View mSpacer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWallpaperAsset = mWallpaper.getAsset(requireContext().getApplicationContext());
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_image_preview;
    }

    @Override
    protected int getBottomSheetResId() {
        return R.id.bottom_sheet;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Activity activity = requireActivity();
        // Set toolbar as the action bar.

        mFullResImageView = view.findViewById(R.id.full_res_image);
        mLoadingIndicator = view.findViewById(R.id.loading_indicator);

        mAttributionTitle = view.findViewById(R.id.preview_attribution_pane_title);
        mAttributionSubtitle1 = view.findViewById(R.id.preview_attribution_pane_subtitle1);
        mAttributionSubtitle2 = view.findViewById(R.id.preview_attribution_pane_subtitle2);
        mExploreButton = view.findViewById(R.id.preview_attribution_pane_explore_button);
        mSetWallpaperButton = view.findViewById(R.id.preview_attribution_pane_set_wallpaper_button);

        mLowResImageView = view.findViewById(R.id.low_res_image);
        mSpacer = view.findViewById(R.id.spacer);

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

        // Configure loading indicator with a MaterialProgressDrawable.
        setUpLoadingIndicator();

        return view;
    }

    @Override
    protected void setUpBottomSheetView(ViewGroup bottomSheet) {
        // Nothing needed here.
    }

    private void setUpLoadingIndicator() {
        Context context = requireContext();
        mProgressDrawable = new MaterialProgressDrawable(context.getApplicationContext(),
                mLoadingIndicator);
        mProgressDrawable.setAlpha(255);
        mProgressDrawable.setBackgroundColor(getResources().getColor(R.color.material_white_100,
                context.getTheme()));
        mProgressDrawable.setColorSchemeColors(getAttrColor(
                new ContextThemeWrapper(context, getDeviceDefaultTheme()),
                android.R.attr.colorAccent));
        mProgressDrawable.updateSizes(MaterialProgressDrawable.LARGE);
        mLoadingIndicator.setImageDrawable(mProgressDrawable);

        // We don't want to show the spinner every time we load an image if it loads quickly;
        // instead, only start showing the spinner if loading the image has taken longer than half
        // of a second.
        mLoadingIndicator.postDelayed(() -> {
            if (mFullResImageView != null && !mFullResImageView.hasImage()
                    && !mTestingModeEnabled) {
                mLoadingIndicator.setVisibility(View.VISIBLE);
                mLoadingIndicator.setAlpha(1f);
                if (mProgressDrawable != null) {
                    mProgressDrawable.start();
                }
            }
        }, 500);
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
        if (mProgressDrawable != null) {
            mProgressDrawable.stop();
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
        mExploreButton.setAlpha(alpha);
        mAttributionTitle.setAlpha(alpha);
        mAttributionSubtitle1.setAlpha(alpha);
        mAttributionSubtitle2.setAlpha(alpha);
    }

    private void populateAttributionPane() {
        final Context context = getContext();

        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);

        List<String> attributions = mWallpaper.getAttributions(context);
        if (attributions.size() > 0 && attributions.get(0) != null) {
            mAttributionTitle.setText(attributions.get(0));
        }

        if (attributions.size() > 1 && attributions.get(1) != null) {
            mAttributionSubtitle1.setVisibility(View.VISIBLE);
            mAttributionSubtitle1.setText(attributions.get(1));
        }

        if (attributions.size() > 2 && attributions.get(2) != null) {
            mAttributionSubtitle2.setVisibility(View.VISIBLE);
            mAttributionSubtitle2.setText(attributions.get(2));
        }

        setUpSetWallpaperButton(mSetWallpaperButton);

        setUpExploreButton(mExploreButton);

        if (mExploreButton.getVisibility() == View.VISIBLE
                && mSetWallpaperButton.getVisibility() == View.VISIBLE) {
            mSpacer.setVisibility(View.VISIBLE);
        } else {
            mSpacer.setVisibility(View.GONE);
        }

        mBottomSheet.setVisibility(View.VISIBLE);

        // Initialize the state of the BottomSheet based on the current state because if the initial
        // and current state are the same, the state change listener won't fire and set the correct
        // arrow asset and text alpha.
        if (bottomSheetBehavior.getState() == STATE_EXPANDED) {
            setPreviewChecked(false);
            mAttributionTitle.setAlpha(1f);
            mAttributionSubtitle1.setAlpha(1f);
            mAttributionSubtitle2.setAlpha(1f);
        } else {
            setPreviewChecked(true);
            mAttributionTitle.setAlpha(0f);
            mAttributionSubtitle1.setAlpha(0f);
            mAttributionSubtitle2.setAlpha(0f);
        }

        // Let the state change listener take care of animating a state change to the initial state
        // if there's a state change.
        bottomSheetBehavior.setState(mBottomSheetInitialState);
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
                    if (mLoadingIndicator != null) {
                        mLoadingIndicator.setVisibility(View.GONE);
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
                    if (mProgressDrawable != null) {
                        mProgressDrawable.stop();
                    }
                    getActivity().invalidateOptionsMenu();

                    populateAttributionPane();
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

        mLoadingIndicator.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mLoadingIndicator != null) {
                            mLoadingIndicator.setVisibility(View.GONE);
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
