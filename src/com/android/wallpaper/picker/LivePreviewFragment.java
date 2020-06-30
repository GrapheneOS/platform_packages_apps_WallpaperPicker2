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

import static com.android.wallpaper.widget.BottomActionBar.BottomAction.APPLY;
import static com.android.wallpaper.widget.BottomActionBar.BottomAction.CUSTOMIZE;
import static com.android.wallpaper.widget.BottomActionBar.BottomAction.DELETE;
import static com.android.wallpaper.widget.BottomActionBar.BottomAction.INFORMATION;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperColors;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.WallpaperService;
import android.service.wallpaper.WallpaperSettingsActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.lifecycle.LiveData;
import androidx.slice.Slice;
import androidx.slice.widget.SliceLiveData;
import androidx.slice.widget.SliceView;

import com.android.wallpaper.R;
import com.android.wallpaper.compat.BuildCompat;
import com.android.wallpaper.model.LiveWallpaperInfo;
import com.android.wallpaper.module.WallpaperPersister.SetWallpaperCallback;
import com.android.wallpaper.util.ScreenSizeCalculator;
import com.android.wallpaper.util.SizeCalculator;
import com.android.wallpaper.util.WallpaperConnection;
import com.android.wallpaper.widget.BottomActionBar;
import com.android.wallpaper.widget.BottomActionBar.AccessibilityCallback;
import com.android.wallpaper.widget.LiveTileOverlay;
import com.android.wallpaper.widget.LockScreenPreviewer;
import com.android.wallpaper.widget.WallpaperInfoView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment which displays the UI for previewing an individual live wallpaper, its attribution
 * information and settings slices if available.
 */
public class LivePreviewFragment extends PreviewFragment implements
        WallpaperConnection.WallpaperConnectionListener {

    public static final String EXTRA_LIVE_WALLPAPER_INFO = "android.live_wallpaper.info";
    public static final String KEY_ACTION_DELETE_LIVE_WALLPAPER = "action_delete_live_wallpaper";

    private static final String TAG = "LivePreviewFragment";

    /**
     * Instance of {@link WallpaperConnection} used to bind to the live wallpaper service to show
     * it in this preview fragment.
     * @see IWallpaperConnection
     */
    protected WallpaperConnection mWallpaperConnection;
    protected WallpaperInfoView mWallpaperInfoView;
    protected CardView mHomePreviewCard;
    protected ImageView mHomePreview;

    private final int[] mLivePreviewLocation = new int[2];
    private final Rect mPreviewLocalRect = new Rect();
    private final Rect mPreviewGlobalRect = new Rect();

    private Intent mDeleteIntent;
    private Intent mSettingsIntent;

    private List<Pair<String, View>> mPages;
    private SliceView mSettingsSliceView;
    private LiveData<Slice> mSettingsLiveData;
    private View mLoadingScrim;
    private InfoPageController mInfoPageController;
    private Point mScreenSize;
    private ViewGroup mPreviewContainer;
    private TouchForwardingLayout mTouchForwardingLayout;
    private View mTab;
    private TextView mHomeTextView;
    private TextView mLockTextView;
    private SurfaceView mWorkspaceSurface;
    private ViewGroup mLockPreviewContainer;
    private LockScreenPreviewer mLockScreenPreviewer;
    private WorkspaceSurfaceHolderCallback mWorkspaceSurfaceCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.app.WallpaperInfo info = mWallpaper.getWallpaperComponent();
        setUpExploreIntentAndLabel(null);

        String deleteAction = getDeleteAction(info);
        if (!TextUtils.isEmpty(deleteAction)) {
            mDeleteIntent = new Intent(deleteAction);
            mDeleteIntent.setPackage(info.getPackageName());
            mDeleteIntent.putExtra(EXTRA_LIVE_WALLPAPER_INFO, info);
        }

        String settingsActivity = getSettingsActivity(info);
        if (settingsActivity != null) {
            mSettingsIntent = new Intent();
            mSettingsIntent.setComponent(new ComponentName(info.getPackageName(),
                    settingsActivity));
            mSettingsIntent.putExtra(WallpaperSettingsActivity.EXTRA_PREVIEW_MODE, true);
            PackageManager pm = requireContext().getPackageManager();
            ActivityInfo activityInfo = mSettingsIntent.resolveActivityInfo(pm, 0);
            if (activityInfo == null) {
                Log.i(TAG, "Couldn't find wallpaper settings activity: " + settingsActivity);
                mSettingsIntent = null;
            }
        }
    }

    @Nullable
    protected String getSettingsActivity(WallpaperInfo info) {
        return info.getSettingsActivity();
    }

    protected Intent getWallpaperIntent(WallpaperInfo info) {
        return new Intent(WallpaperService.SERVICE_INTERFACE)
                .setClassName(info.getPackageName(), info.getServiceName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mPages = new ArrayList<>();
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            return null;
        }

        Activity activity = requireActivity();

        mLoadingScrim = view.findViewById(R.id.loading);
        setUpLoadingIndicator();

        mScreenSize = ScreenSizeCalculator.getInstance().getScreenSize(
                activity.getWindowManager().getDefaultDisplay());

        mPreviewContainer = view.findViewById(R.id.live_wallpaper_preview);
        mHomePreviewCard = mPreviewContainer.findViewById(R.id.wallpaper_full_preview_card);
        mTouchForwardingLayout = view.findViewById(R.id.touch_forwarding_layout);
        // Set aspect ratio on the preview card.
        ConstraintSet set = new ConstraintSet();
        set.clone((ConstraintLayout) mPreviewContainer);
        String ratio = String.format("%d:%d", mScreenSize.x, mScreenSize.y);
        set.setDimensionRatio(mTouchForwardingLayout.getId(), ratio);
        set.applyTo((ConstraintLayout) mPreviewContainer);

        mHomePreview = mHomePreviewCard.findViewById(R.id.wallpaper_preview_image);
        mTouchForwardingLayout.setTargetView(mHomePreview);
        mTouchForwardingLayout.setForwardingEnabled(true);
        mLockPreviewContainer = mPreviewContainer.findViewById(R.id.lock_screen_preview_container);
        mLockScreenPreviewer = new LockScreenPreviewer(getLifecycle(), getActivity(),
                mLockPreviewContainer);
        mTab = view.findViewById(R.id.tabs_container);
        mHomeTextView = mTab.findViewById(R.id.home);
        mLockTextView = mTab.findViewById(R.id.lock);
        mWorkspaceSurface = mHomePreviewCard.findViewById(R.id.workspace_surface);
        mWorkspaceSurfaceCallback = new WorkspaceSurfaceHolderCallback(
                mWorkspaceSurface, getContext());
        updateScreenTab(/* isHomeSelected= */ mViewAsHome);
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View thisView, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                setupPreview();
                view.removeOnLayoutChangeListener(this);
            }
        });
        onBottomActionBarReady(mBottomActionBar);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupCurrentWallpaperPreview(view);
        renderWorkspaceSurface();
    }

    private void updateScreenTab(boolean isHomeSelected) {
        mHomeTextView.setSelected(isHomeSelected);
        mLockTextView.setSelected(!isHomeSelected);
        mWorkspaceSurface.setVisibility(isHomeSelected ? View.VISIBLE : View.INVISIBLE);
        mLockPreviewContainer.setVisibility(isHomeSelected ? View.INVISIBLE : View.VISIBLE);
    }

    private void setupPreview() {
        mHomeTextView.setOnClickListener(view ->
                updateScreenTab(/* isHomeSelected= */ true)
        );
        mLockTextView.setOnClickListener(view ->
                updateScreenTab(/* isHomeSelected= */ false)
        );
        if (mWallpaperInfoView != null) {
            mWallpaperInfoView.populateWallpaperInfo(
                    mWallpaper,
                    mActionLabel,
                    mExploreIntent,
                    LivePreviewFragment.this::onExploreClicked);
        }

        ((CardView) mHomePreview.getParent())
                .setRadius(SizeCalculator.getPreviewCornerRadius(
                        getActivity(), mHomePreviewCard.getMeasuredWidth()));
    }

    private void repositionPreview(ImageView previewView) {
        previewView.getLocationOnScreen(mLivePreviewLocation);
        mPreviewGlobalRect.set(0, 0, previewView.getMeasuredWidth(),
                previewView.getMeasuredHeight());
        mPreviewLocalRect.set(mPreviewGlobalRect);
        mPreviewGlobalRect.offset(mLivePreviewLocation[0], mLivePreviewLocation[1]);
    }

    private void setupCurrentWallpaperPreview(View view) {
        mHomePreview.setOnTouchListener((v, ev) -> {
            if (mWallpaperConnection != null && mWallpaperConnection.getEngine() != null) {
                int action = ev.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    mBottomActionBar.collapseBottomSheetIfExpanded();
                }
                MotionEvent dup = MotionEvent.obtainNoHistory(ev);
                try {
                    mWallpaperConnection.getEngine().dispatchPointer(dup);
                    if (action == MotionEvent.ACTION_UP) {
                        mWallpaperConnection.getEngine().dispatchWallpaperCommand(
                                WallpaperManager.COMMAND_TAP,
                                (int) ev.getX(), (int) ev.getY(), 0, null);
                    } else if (action == MotionEvent.ACTION_POINTER_UP) {
                        int pointerIndex = ev.getActionIndex();
                        mWallpaperConnection.getEngine().dispatchWallpaperCommand(
                                WallpaperManager.COMMAND_SECONDARY_TAP,
                                (int) ev.getX(pointerIndex), (int) ev.getY(pointerIndex), 0, null);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Remote exception of wallpaper connection");
                }
            }
            return false;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSettingsLiveData != null && mSettingsLiveData.hasObservers()) {
            mSettingsLiveData.removeObserver(mSettingsSliceView);
            mSettingsLiveData = null;
        }
        LiveTileOverlay.INSTANCE.detach(mHomePreview.getOverlay());
        if (mWallpaperConnection != null) {
            mWallpaperConnection.disconnect();
        }
        mWallpaperConnection = null;

        mWorkspaceSurfaceCallback.cleanUp();

        super.onDestroy();
    }

    private void previewLiveWallpaper(ImageView thumbnailView) {
        thumbnailView.post(() -> {
            mWallpaper.getThumbAsset(requireActivity().getApplicationContext()).loadPreviewImage(
                    requireActivity(), thumbnailView,
                    getResources().getColor(R.color.secondary_color));
            LiveTileOverlay.INSTANCE.detach(thumbnailView.getOverlay());

            setUpLiveWallpaperPreview(mWallpaper, thumbnailView,
                    new ColorDrawable(getResources().getColor(
                            R.color.secondary_color, getActivity().getTheme())));
        });
    }

    private void setUpLiveWallpaperPreview(com.android.wallpaper.model.WallpaperInfo homeWallpaper,
            ImageView previewView, Drawable thumbnail) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (mWallpaperConnection != null) {
            mWallpaperConnection.disconnect();
        }
        if (thumbnail != null) {
            thumbnail.setBounds(previewView.getLeft(), previewView.getTop(), previewView.getRight(),
                    previewView.getBottom());
        }
        repositionPreview(previewView);

        mWallpaperConnection = new WallpaperConnection(
                getWallpaperIntent(homeWallpaper.getWallpaperComponent()), activity,
                new WallpaperConnection.WallpaperConnectionListener() {
                    @Override
                    public void onEngineShown() {
                        Context context = getContext();
                        if (context == null) {
                            return;
                        }
                        mLoadingScrim.post(() -> mLoadingScrim.animate()
                                .alpha(0f)
                                .setDuration(250)
                                .setStartDelay(200)
                                .setInterpolator(AnimationUtils.loadInterpolator(context,
                                        android.R.interpolator.fast_out_linear_in))
                                .withEndAction(() -> {
                                    if (mLoadingProgressBar != null) {
                                        mLoadingProgressBar.hide();
                                    }
                                    mLoadingScrim.setVisibility(View.GONE);
                                }));
                        final Drawable placeholder = previewView.getDrawable() == null
                                ? new ColorDrawable(getResources().getColor(R.color.secondary_color,
                                activity.getTheme()))
                                : previewView.getDrawable();
                        LiveTileOverlay.INSTANCE.setForegroundDrawable(placeholder);
                        LiveTileOverlay.INSTANCE.attach(previewView.getOverlay());
                        previewView.animate()
                                .setStartDelay(0)
                                .setDuration(150)
                                .setInterpolator(AnimationUtils.loadInterpolator(context,
                                        android.R.interpolator.fast_out_linear_in))
                                .setUpdateListener(value -> placeholder.setAlpha(
                                        (int) (255 * (1 - value.getAnimatedFraction()))))
                                .withEndAction(() -> {
                                    LiveTileOverlay.INSTANCE.setForegroundDrawable(null);
                                }).start();
                    }

                    @Override
                    public void onWallpaperColorsChanged(WallpaperColors colors, int displayId) {
                        mLockScreenPreviewer.setColor(colors);
                    }
                }, mPreviewGlobalRect);

        LiveTileOverlay.INSTANCE.update(new RectF(mPreviewLocalRect),
                ((CardView) previewView.getParent()).getRadius());

        mWallpaperConnection.setVisibility(true);
        mLoadingScrim.post(() -> mLoadingScrim.animate()
                .alpha(0f)
                .setStartDelay(50)
                .setDuration(250)
                .setInterpolator(AnimationUtils.loadInterpolator(getContext(),
                        android.R.interpolator.fast_out_linear_in))
                .withEndAction(() -> {
                    if (mWallpaperConnection != null && !mWallpaperConnection.connect()) {
                        mWallpaperConnection = null;
                        LiveTileOverlay.INSTANCE.detach(previewView.getOverlay());
                    }
                }));
    }

    private void renderWorkspaceSurface() {
        mWorkspaceSurface.setZOrderMediaOverlay(true);
        mWorkspaceSurface.getHolder().addCallback(mWorkspaceSurfaceCallback);
    }

    protected void onBottomActionBarReady(BottomActionBar bottomActionBar) {
        super.onBottomActionBarReady(bottomActionBar);
        mBottomActionBar.showActionsOnly(INFORMATION, DELETE, CUSTOMIZE, APPLY);
        mBottomActionBar.bindBackButtonToSystemBackKey(getActivity());
        mBottomActionBar.setActionClickListener(APPLY, unused ->
                this.onSetWallpaperClicked(null));
        mWallpaperInfoView = (WallpaperInfoView) LayoutInflater.from(getContext())
                .inflate(R.layout.wallpaper_info_view, /* root= */ null);
        mBottomActionBar.attachViewToBottomSheetAndBindAction(mWallpaperInfoView, INFORMATION);

        // Update target view's accessibility param since it will be blocked by the bottom sheet
        // when expanded.
        mBottomActionBar.setAccessibilityCallback(new AccessibilityCallback() {
            @Override
            public void onBottomSheetCollapsed() {
                mTab.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }

            @Override
            public void onBottomSheetExpanded() {
                mTab.setImportantForAccessibility(
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }
        });
        final Uri uriSettingsSlice = getSettingsSliceUri(mWallpaper.getWallpaperComponent());
        if (uriSettingsSlice != null) {
            View previewPage = LayoutInflater.from(getContext())
                    .inflate(R.layout.preview_customize_settings, null);
            mSettingsSliceView = previewPage.findViewById(R.id.settings_slice);
            mSettingsSliceView.setMode(SliceView.MODE_LARGE);
            mSettingsSliceView.setScrollable(false);
            mSettingsLiveData = SliceLiveData.fromUri(requireContext(), uriSettingsSlice);
            mSettingsLiveData.observeForever(mSettingsSliceView);
            mBottomActionBar.attachViewToBottomSheetAndBindAction(previewPage, CUSTOMIZE);
        } else {
            if (mSettingsIntent != null) {
                mBottomActionBar.setActionClickListener(CUSTOMIZE, listener ->
                        startActivity(mSettingsIntent));
            } else {
                mBottomActionBar.hideActions(CUSTOMIZE);
            }
        }

        final String deleteAction = getDeleteAction(mWallpaper.getWallpaperComponent());
        if (TextUtils.isEmpty(deleteAction)) {
            mBottomActionBar.hideActions(DELETE);
        } else {
            mBottomActionBar.setActionClickListener(DELETE, listener ->
                    showDeleteConfirmDialog());
        }
        mBottomActionBar.show();
    }

    @Override
    public void onEngineShown() {
        mLoadingScrim.post(() -> mLoadingScrim.animate()
                .alpha(0f)
                .setDuration(220)
                .setStartDelay(300)
                .setInterpolator(AnimationUtils.loadInterpolator(getActivity(),
                        android.R.interpolator.fast_out_linear_in))
                .withEndAction(() -> {
                    if (mLoadingProgressBar != null) {
                        mLoadingProgressBar.hide();
                    }
                    mLoadingScrim.setVisibility(View.INVISIBLE);
                    populateInfoPage(mInfoPageController);
                }));
    }

    @Override
    protected boolean isLoaded() {
        return mWallpaperConnection != null && mWallpaperConnection.isEngineReady();
    }

    @Override
    protected CharSequence getExploreButtonLabel(Context context) {
        CharSequence exploreLabel = ((LiveWallpaperInfo) mWallpaper).getActionDescription(context);
        if (TextUtils.isEmpty(exploreLabel)) {
            exploreLabel = context.getString(mWallpaper.getActionLabelRes(context));
        }
        return exploreLabel;
    }

    @SuppressLint("NewApi") //Already checking with isAtLeastQ
    protected Uri getSettingsSliceUri(android.app.WallpaperInfo info) {
        if (BuildCompat.isAtLeastQ()) {
            return info.getSettingsSliceUri();
        }
        return null;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_live_preview_v2;
    }

    @Override
    protected int getLoadingIndicatorResId() {
        return R.id.loading_indicator;
    }

    @Override
    protected void setCurrentWallpaper(int destination) {
        mWallpaperSetter.setCurrentWallpaper(getActivity(), mWallpaper, null,
                destination, 0, null, new SetWallpaperCallback() {
                    @Override
                    public void onSuccess(com.android.wallpaper.model.WallpaperInfo wallpaperInfo) {
                        finishActivity(/* success= */ true);
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable) {
                        showSetWallpaperErrorDialog(destination);
                    }
                });
    }

    @Nullable
    protected String getDeleteAction(android.app.WallpaperInfo wallpaperInfo) {
        android.app.WallpaperInfo currentInfo =
                WallpaperManager.getInstance(requireContext()).getWallpaperInfo();
        ServiceInfo serviceInfo = wallpaperInfo.getServiceInfo();
        if (!isPackagePreInstalled(serviceInfo.applicationInfo)) {
            Log.d(TAG, "This wallpaper is not pre-installed: " + serviceInfo.name);
            return null;
        }

        ServiceInfo currentService = currentInfo == null ? null : currentInfo.getServiceInfo();
        // A currently set Live wallpaper should not be deleted.
        if (currentService != null && TextUtils.equals(serviceInfo.name, currentService.name)) {
            return null;
        }

        final Bundle metaData = serviceInfo.metaData;
        if (metaData != null) {
            return metaData.getString(KEY_ACTION_DELETE_LIVE_WALLPAPER);
        }
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        previewLiveWallpaper(mHomePreview);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWallpaperConnection != null) {
            mWallpaperConnection.setVisibility(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWallpaperConnection != null) {
            mWallpaperConnection.setVisibility(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mWallpaperConnection != null) {
            mWallpaperConnection.disconnect();
            mWallpaperConnection = null;
        }
    }

    private void showDeleteConfirmDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setMessage(R.string.delete_wallpaper_confirmation)
                .setOnDismissListener(dialog -> mBottomActionBar.deselectAction(DELETE))
                .setPositiveButton(R.string.delete_live_wallpaper,
                        (dialog, which) -> deleteLiveWallpaper())
                .setNegativeButton(android.R.string.cancel, null /* listener */)
                .create();
        alertDialog.show();
    }

    private void deleteLiveWallpaper() {
        if (mDeleteIntent != null) {
            requireContext().startService(mDeleteIntent);
            finishActivity(/* success= */ false);
        }
    }

    private boolean isPackagePreInstalled(ApplicationInfo info) {
        if (info != null && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            return true;
        }
        return false;
    }
}
