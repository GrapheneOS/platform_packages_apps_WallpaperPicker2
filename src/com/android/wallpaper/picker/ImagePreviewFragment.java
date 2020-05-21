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

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import static com.android.wallpaper.widget.BottomActionBar.BottomAction.APPLY;
import static com.android.wallpaper.widget.BottomActionBar.BottomAction.EDIT;
import static com.android.wallpaper.widget.BottomActionBar.BottomAction.INFORMATION;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.wallpaper.R;
import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.WallpaperPersister.Destination;
import com.android.wallpaper.module.WallpaperPersister.SetWallpaperCallback;
import com.android.wallpaper.util.ScreenSizeCalculator;
import com.android.wallpaper.util.SizeCalculator;
import com.android.wallpaper.util.WallpaperCropUtils;
import com.android.wallpaper.widget.BottomActionBar;
import com.android.wallpaper.widget.LockScreenOverlayUpdater;
import com.android.wallpaper.widget.WallpaperColorsLoader;
import com.android.wallpaper.widget.WallpaperInfoView;

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

    private final Handler mHandler = new Handler();

    private SubsamplingScaleImageView mFullResImageView;
    private Asset mWallpaperAsset;
    private Point mDefaultCropSurfaceSize;
    private Point mScreenSize;
    private Point mRawWallpaperSize; // Native size of wallpaper image.
    private ImageView mLowResImageView;
    private TouchForwardingLayout mTouchForwardingLayout;
    private ConstraintLayout mContainer;
    private SurfaceView mWorkspaceSurface;
    private WorkspaceSurfaceHolderCallback mWorkspaceSurfaceCallback;
    private SurfaceView mWallpaperSurface;
    private View mLockOverlay;
    private LockScreenOverlayUpdater mLockScreenOverlayUpdater;
    private View mTabs;
    private BottomActionBar mBottomActionBar;
    private WallpaperInfoView mWallpaperInfoView;
    private InfoPageController mInfoPageController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWallpaperAsset = mWallpaper.getAsset(requireContext().getApplicationContext());
    }

    @Override
    protected int getLayoutResId() {
        return USE_NEW_UI ? R.layout.fragment_image_preview_v2 : R.layout.fragment_image_preview;
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
        mScreenSize = ScreenSizeCalculator.getInstance().getScreenSize(
                activity.getWindowManager().getDefaultDisplay());

        if (USE_NEW_UI) {
            // TODO: Consider moving some part of this to the base class when live preview is ready.
            view.findViewById(R.id.low_res_image).setVisibility(View.GONE);
            view.findViewById(R.id.full_res_image).setVisibility(View.GONE);
            mLoadingProgressBar.hide();
            mContainer = view.findViewById(R.id.container);
            mTouchForwardingLayout = mContainer.findViewById(R.id.touch_forwarding_layout);

            // Set aspect ratio on the preview card dynamically.
            ConstraintSet set = new ConstraintSet();
            set.clone(mContainer);
            String ratio = String.format("%d:%d", mScreenSize.x, mScreenSize.y);
            set.setDimensionRatio(mTouchForwardingLayout.getId(), ratio);
            set.applyTo(mContainer);

            mWorkspaceSurface = mContainer.findViewById(R.id.workspace_surface);
            mWorkspaceSurfaceCallback = new WorkspaceSurfaceHolderCallback(mWorkspaceSurface,
                    getContext());
            mWallpaperSurface = mContainer.findViewById(R.id.wallpaper_surface);
            mLockOverlay = mContainer.findViewById(R.id.lock_overlay);
            mLockScreenOverlayUpdater = new LockScreenOverlayUpdater(
                    getContext(), mLockOverlay, getLifecycle());
            mLockScreenOverlayUpdater.adjustOverlayLayout(true);

            mTabs = view.findViewById(R.id.tabs_container);
            View lock = mTabs.findViewById(R.id.lock);
            View home = mTabs.findViewById(R.id.home);
            lock.setOnClickListener(v -> showLockscreenPreview());
            home.setOnClickListener(v -> showHomescreenPreview());
            mBottomActionBar = view.findViewById(R.id.bottom_actionbar);
            mWallpaperInfoView = (WallpaperInfoView)
                    inflater.inflate(R.layout.wallpaper_info_view, /* root= */ null);
            mBottomActionBar.attachViewToBottomSheetAndBindAction(mWallpaperInfoView, INFORMATION);
            mBottomActionBar.showActionsOnly(INFORMATION, EDIT, APPLY);
            mBottomActionBar.bindBackButtonToSystemBackKey(getActivity());
            mBottomActionBar.setActionClickListener(EDIT, v -> {
                setEditingEnabled(mBottomActionBar.isActionSelected(EDIT));
            });
            mBottomActionBar.setActionClickListener(APPLY, v -> {
                onSetWallpaperClicked(v);
                setEditingEnabled(false);
            });
            mBottomActionBar.show();
            view.measure(makeMeasureSpec(mScreenSize.x, EXACTLY),
                    makeMeasureSpec(mScreenSize.y, EXACTLY));

            ((CardView) mWorkspaceSurface.getParent())
                    .setRadius(SizeCalculator.getPreviewCornerRadius(
                            activity, mContainer.getMeasuredWidth()));
            renderImageWallpaper();
            renderWorkspaceSurface();
        } else {
            mFullResImageView = view.findViewById(R.id.full_res_image);
            mLowResImageView = view.findViewById(R.id.low_res_image);
        }

        mInfoPageController = new InfoPageController(view.findViewById(R.id.page_info),
                mPreviewMode);

        // Trim some memory from Glide to make room for the full-size image in this fragment.
        Glide.get(activity).setMemoryCategory(MemoryCategory.LOW);

        mDefaultCropSurfaceSize = WallpaperCropUtils.getDefaultCropSurfaceSize(
                getResources(), activity.getWindowManager().getDefaultDisplay());

        // Load a low-res placeholder image if there's a thumbnail available from the asset that can
        // be shown to the user more quickly than the full-sized image.
        if (mWallpaperAsset.hasLowResDataSource()) {
            if (!USE_NEW_UI) {
                mWallpaperAsset.loadLowResDrawable(activity, mLowResImageView, Color.BLACK,
                        new WallpaperPreviewBitmapTransformation(
                                activity.getApplicationContext(), isRtl()));
            } else {
                mHandler.post(() ->
                        mWallpaperAsset.loadLowResDrawable(activity, mLowResImageView, Color.BLACK,
                                new WallpaperPreviewBitmapTransformation(
                                        activity.getApplicationContext(), isRtl())));
            }
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

            if (USE_NEW_UI) {
                setUpExploreIntentAndLabel(ImagePreviewFragment.this::initFullResView);
            } else {
                setUpExploreIntent(ImagePreviewFragment.this::initFullResView);
            }
        });

        setUpLoadingIndicator();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (USE_NEW_UI) {
            WallpaperColorsLoader.getWallpaperColors(
                    getContext(),
                    mWallpaperAsset,
                    mWallpaperSurface.getMeasuredWidth(),
                    mWallpaperSurface.getMeasuredHeight(),
                    mLockScreenOverlayUpdater::setColor);
        }
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
        if (USE_NEW_UI) {
            mWorkspaceSurfaceCallback.cleanUp();
        }
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
        setEditingEnabled(!USE_NEW_UI);
        mFullResImageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);

        // Set a solid black "page bitmap" so MosaicView draws a black background while waiting
        // for the image to load or a transparent one if a thumbnail already loaded.
        Bitmap backgroundBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        int preColor = USE_NEW_UI ? ContextCompat.getColor(getContext(),
                R.color.fullscreen_preview_background) : Color.BLACK;
        int color = (mLowResImageView.getDrawable() == null) ? preColor : Color.TRANSPARENT;
        backgroundBitmap.setPixel(0, 0, color);
        mFullResImageView.setImage(ImageSource.bitmap(backgroundBitmap));

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

                    if (USE_NEW_UI) {
                        if (mWallpaperInfoView != null && mWallpaper != null) {
                            mWallpaperInfoView.populateWallpaperInfo(mWallpaper, mActionLabel,
                                    mExploreIntent, this::onExploreClicked);
                        }
                    } else {
                        populateInfoPage(mInfoPageController);
                    }
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
     * Sets the default wallpaper zoom and scroll position based on a "crop surface" (with extra
     * width to account for parallax) superimposed on the screen. Shows as much of the wallpaper as
     * possible on the crop surface and align screen to crop surface such that the default preview
     * matches what would be seen by the user in the left-most home screen.
     *
     * <p>This method is called once in the Fragment lifecycle after the wallpaper asset has loaded
     * and rendered to the layout.
     */
    private void setDefaultWallpaperZoomAndScroll() {
        final float defaultWallpaperZoom;
        final float minWallpaperZoom;
        final PointF centerPosition;

        if (USE_NEW_UI) {
            // Determine minimum zoom to fit maximum visible area of wallpaper on crop surface.
            int cropWidth = mWorkspaceSurface.getMeasuredWidth();
            int cropHeight = mWorkspaceSurface.getMeasuredHeight();
            Point crop = new Point(cropWidth, cropHeight);
            Rect visibleRawWallpaperRect =
                    WallpaperCropUtils.calculateVisibleRect(mRawWallpaperSize, crop);
            WallpaperCropUtils.adjustCurrentWallpaperCropRect(getContext(), mRawWallpaperSize,
                    visibleRawWallpaperRect);
            Point visibleRawWallpaperSize = new Point(visibleRawWallpaperRect.width(),
                    visibleRawWallpaperRect.height());

            defaultWallpaperZoom = WallpaperCropUtils.calculateMinZoom(
                    visibleRawWallpaperSize, crop);
            minWallpaperZoom = defaultWallpaperZoom;

            centerPosition = new PointF(visibleRawWallpaperRect.centerX(),
                    visibleRawWallpaperRect.centerY());
        } else {
            // Determine minimum zoom to fit maximum visible area of wallpaper on crop surface.
            defaultWallpaperZoom = WallpaperCropUtils.calculateMinZoom(
                    mRawWallpaperSize, mDefaultCropSurfaceSize);
            minWallpaperZoom =
                    WallpaperCropUtils.calculateMinZoom(mRawWallpaperSize, mScreenSize);

            centerPosition = new PointF(mRawWallpaperSize.x / 2f, mRawWallpaperSize.y / 2f);
        }

        // Set min wallpaper zoom and max zoom on MosaicView widget.
        mFullResImageView.setMaxScale(Math.max(DEFAULT_WALLPAPER_MAX_ZOOM, defaultWallpaperZoom));
        mFullResImageView.setMinScale(minWallpaperZoom);

        // Set center to composite positioning between scaled wallpaper and screen.
        mFullResImageView.setScaleAndCenter(minWallpaperZoom, centerPosition);
    }

    private Rect calculateCropRect() {
        float wallpaperZoom = mFullResImageView.getScale();
        Context context = requireContext().getApplicationContext();
        Display defaultDisplay = requireActivity().getWindowManager().getDefaultDisplay();

        Rect visibleFileRect = new Rect();
        mFullResImageView.visibleFileRect(visibleFileRect);

        if (USE_NEW_UI) {
            int cropWidth = mWorkspaceSurface.getMeasuredWidth();
            int cropHeight = mWorkspaceSurface.getMeasuredHeight();
            Point hostViewSize = new Point(cropWidth, cropHeight);
            Rect cropRect = WallpaperCropUtils.calculateCropRect(context, hostViewSize,
                    hostViewSize, mRawWallpaperSize, visibleFileRect, wallpaperZoom);
            WallpaperCropUtils.adjustCropRect(context, cropRect, false /* zoomIn */);
            return cropRect;
        }
        return WallpaperCropUtils.calculateCropRect(context, defaultDisplay, mRawWallpaperSize,
                visibleFileRect, wallpaperZoom);
    }

    @Override
    protected void setCurrentWallpaper(@Destination int destination) {
        mWallpaperSetter.setCurrentWallpaper(getActivity(), mWallpaper, mWallpaperAsset,
                destination, mFullResImageView.getScale(), calculateCropRect(),
                new SetWallpaperCallback() {
                    @Override
                    public void onSuccess(WallpaperInfo wallpaperInfo) {
                        finishActivityWithResultOk();
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable) {
                        showSetWallpaperErrorDialog(destination);
                    }
                });
    }

    private void renderWorkspaceSurface() {
        mWorkspaceSurface.setZOrderMediaOverlay(true);
        mWorkspaceSurface.getHolder().addCallback(mWorkspaceSurfaceCallback);
    }

    private void renderImageWallpaper() {
        mWallpaperSurface.getHolder().addCallback(mWallpaperSurfaceCallback);
    }

    // TODO(tracyzhou): Refactor this into a utility class.
    private final SurfaceHolder.Callback mWallpaperSurfaceCallback = new SurfaceHolder.Callback() {

        private Surface mLastSurface;

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (mLastSurface != holder.getSurface()) {
                mLastSurface = holder.getSurface();
                if (mFullResImageView != null) {
                    mFullResImageView.recycle();
                }
                View wallpaperPreviewContainer = LayoutInflater.from(getContext()).inflate(
                        R.layout.fullscreen_wallpaper_preview, null);
                mFullResImageView = wallpaperPreviewContainer.findViewById(R.id.full_res_image);
                mLowResImageView = wallpaperPreviewContainer.findViewById(R.id.low_res_image);
                wallpaperPreviewContainer.measure(
                        makeMeasureSpec(mWallpaperSurface.getWidth(), EXACTLY),
                        makeMeasureSpec(mWallpaperSurface.getHeight(), EXACTLY));
                wallpaperPreviewContainer.layout(0, 0, mWallpaperSurface.getWidth(),
                        mWallpaperSurface.getHeight());
                mTouchForwardingLayout.setView(mFullResImageView);

                SurfaceControlViewHost host = new SurfaceControlViewHost(getContext(),
                        getContext().getDisplay(), mWallpaperSurface.getHostToken());
                host.setView(wallpaperPreviewContainer, wallpaperPreviewContainer.getWidth(),
                        wallpaperPreviewContainer.getHeight());
                mWallpaperSurface.setChildSurfacePackage(host.getSurfacePackage());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) { }
    };

    private void setEditingEnabled(boolean enabled) {
        if (USE_NEW_UI) {
            mTouchForwardingLayout.setForwardingEnabled(enabled);
        }
    }

    private void showHomescreenPreview() {
        // TODO (b/156129610): Update the behavior here.
        mWorkspaceSurface.setVisibility(View.VISIBLE);
        mLockOverlay.setVisibility(View.GONE);
    }

    private void showLockscreenPreview() {
        // TODO (b/156129610): Update the behavior here.
        mWorkspaceSurface.setVisibility(View.GONE);
        mLockOverlay.setVisibility(View.VISIBLE);
    }
}
