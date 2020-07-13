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
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LiveData;
import androidx.slice.Slice;
import androidx.slice.widget.SliceLiveData;
import androidx.slice.widget.SliceView;
import androidx.viewpager.widget.ViewPager;

import com.android.wallpaper.R;
import com.android.wallpaper.compat.BuildCompat;
import com.android.wallpaper.model.LiveWallpaperInfo;
import com.android.wallpaper.module.WallpaperPersister.SetWallpaperCallback;
import com.android.wallpaper.util.ScreenSizeCalculator;
import com.android.wallpaper.util.SizeCalculator;
import com.android.wallpaper.util.WallpaperConnection;
import com.android.wallpaper.widget.BottomActionBar;
import com.android.wallpaper.widget.LiveTileOverlay;
import com.android.wallpaper.widget.LockScreenOverlayUpdater;
import com.android.wallpaper.widget.WallpaperInfoView;

import com.google.android.material.tabs.TabLayout;

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

    private final int[] mLivePreviewLocation = new int[2];
    private final Rect mPreviewLocalRect = new Rect();
    private final Rect mPreviewGlobalRect = new Rect();

    private Intent mWallpaperIntent;
    private Intent mDeleteIntent;
    private Intent mSettingsIntent;

    private List<Pair<String, View>> mPages;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private SliceView mSettingsSliceView;
    private LiveData<Slice> mSettingsLiveData;
    private View mLoadingScrim;
    private InfoPageController mInfoPageController;
    private Point mScreenSize;
    private ViewGroup mViewGroup;
    private CardView mHomePreviewCard;
    private TouchForwardingLayout mTouchForwardingLayout;
    private ImageView mHomePreview;
    private View mTab;
    private TextView mHomeTextView;
    private TextView mLockTextView;
    private WallpaperInfoView mWallpaperInfoView;
    private SurfaceView mWorkspaceSurface;
    private ViewGroup mLockScreenOverlay;
    private LockScreenOverlayUpdater mLockScreenOverlayUpdater;
    private WorkspaceSurfaceHolderCallback mWorkspaceSurfaceCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.app.WallpaperInfo info = mWallpaper.getWallpaperComponent();
        mWallpaperIntent = getWallpaperIntent(info);
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

        mViewGroup = view.findViewById(R.id.live_wallpaper_preview);
        mHomePreviewCard = mViewGroup.findViewById(R.id.wallpaper_full_preview_card);
        mTouchForwardingLayout = view.findViewById(R.id.touch_forwarding_layout);
        mHomePreview = mHomePreviewCard.findViewById(R.id.wallpaper_preview_image);
        mTouchForwardingLayout.setTargetView(mHomePreview);
        mTouchForwardingLayout.setForwardingEnabled(true);
        mLockScreenOverlay = mViewGroup.findViewById(R.id.lock_overlay);
        mLockScreenOverlayUpdater =
                new LockScreenOverlayUpdater(getContext(), mLockScreenOverlay, getLifecycle());
        mLockScreenOverlayUpdater.adjustOverlayLayout(/* isFullScreen= */ true);
        mTab = view.findViewById(R.id.tabs_container);
        mHomeTextView = mTab.findViewById(R.id.home);
        mHomeTextView.setTextColor(
                getResources().getColorStateList(R.color.bottom_action_button_color_tint,
                        getContext().getTheme()));
        mLockTextView = mTab.findViewById(R.id.lock);
        mLockTextView.setTextColor(
                getResources().getColorStateList(R.color.bottom_action_button_color_tint,
                        getContext().getTheme()));
        mWorkspaceSurface = mHomePreviewCard.findViewById(R.id.workspace_surface);
        mWorkspaceSurfaceCallback = new WorkspaceSurfaceHolderCallback(
                mWorkspaceSurface, getContext());
        updateScreenTab(/* isHomeSelected= */ true);
        view.addOnLayoutChangeListener((thisView, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) ->
                setupPreview()
        );
        setupCurrentWallpaperPreview(view);
        previewLiveWallpaper(container, mHomePreview);
        setupPreview();
        renderWorkspaceSurface();
        onBottomActionBarReady(mBottomActionBar);

        return view;
    }

    private void updateScreenTab(boolean isHomeSelected) {
        mHomeTextView.setSelected(isHomeSelected);
        mLockTextView.setSelected(!isHomeSelected);
        mWorkspaceSurface.setVisibility(isHomeSelected ? View.VISIBLE : View.GONE);
        mLockScreenOverlay.setVisibility(isHomeSelected ? View.GONE : View.VISIBLE);
    }

    private void setupPreview() {
        float screenAspectRatio = (float) mScreenSize.y / mScreenSize.x;
        int measuredViewHeight = mViewGroup.getMeasuredHeight();
        int measuredViewWidth = mViewGroup.getMeasuredWidth();
        int absoluteViewWidth = (int) ((measuredViewHeight - mViewGroup.getPaddingBottom()
                - mViewGroup.getPaddingTop()) / screenAspectRatio);
        int horizontalPadding = (measuredViewWidth - absoluteViewWidth) / 2;
        mViewGroup.setPaddingRelative(
                horizontalPadding,
                mViewGroup.getPaddingTop(),
                horizontalPadding,
                mViewGroup.getPaddingBottom());
        mHomeTextView.setOnClickListener(view ->
                updateScreenTab(/* isHomeSelected= */ true)
        );
        mLockTextView.setOnClickListener(view ->
                updateScreenTab(/* isHomeSelected= */ false)
        );
        repositionPreview(mHomePreview);

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
        if (mWallpaperConnection != null) {
            mWallpaperConnection.updatePreviewPosition(mPreviewGlobalRect);
        }
    }

    private void setupCurrentWallpaperPreview(View view) {
        showCurrentWallpaper(view, /* show= */ true);
        mHomePreview.setOnTouchListener((v, ev) -> {
            if (mWallpaperConnection != null && mWallpaperConnection.getEngine() != null) {
                int action = ev.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    mBottomActionBar.expandBottomSheet(false);
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

    private void showCurrentWallpaper(View rootView, boolean show) {
        rootView.findViewById(R.id.live_wallpaper_preview)
                .setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSettingsLiveData != null && mSettingsLiveData.hasObservers()) {
            mSettingsLiveData.removeObserver(mSettingsSliceView);
            mSettingsLiveData = null;
        }
        if (mWallpaperConnection != null) {
            mWallpaperConnection.disconnect();
        }
        mWallpaperConnection = null;

        mWorkspaceSurfaceCallback.cleanUp();

        super.onDestroy();
    }

    private void previewLiveWallpaper(ViewGroup container, ImageView thumbnailView) {
        container.post(() -> {
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
        if (activity == null) {
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
                                    mLoadingScrim.setVisibility(View.GONE);
                                    if (mWallpaperInfoView != null) {
                                        mWallpaperInfoView.populateWallpaperInfo(
                                                mWallpaper,
                                                mActionLabel,
                                                mExploreIntent,
                                                LivePreviewFragment.this::onExploreClicked);
                                    }
                                }));
                        final Drawable placeholder = previewView.getDrawable() == null
                                ? new ColorDrawable(getResources().getColor(R.color.secondary_color,
                                activity.getTheme()))
                                : previewView.getDrawable();
                        LiveTileOverlay.INSTANCE.setForegroundDrawable(placeholder);
                        LiveTileOverlay.INSTANCE.attach(previewView.getOverlay());
                        previewView.animate()
                                .setStartDelay(50)
                                .setDuration(200)
                                .setInterpolator(AnimationUtils.loadInterpolator(getContext(),
                                        android.R.interpolator.fast_out_linear_in))
                                .setUpdateListener(value -> placeholder.setAlpha(
                                        (int) (255 * (1 - value.getAnimatedFraction()))))
                                .withEndAction(() -> {
                                    LiveTileOverlay.INSTANCE.setForegroundDrawable(null);
                                }).start();
                    }
                }, mPreviewGlobalRect);

        LiveTileOverlay.INSTANCE.update(new RectF(mPreviewLocalRect),
                ((CardView) previewView.getParent()).getRadius());

        mWallpaperConnection.setVisibility(true);
        previewView.post(() -> {
            if (!mWallpaperConnection.connect()) {
                mWallpaperConnection = null;
                LiveTileOverlay.INSTANCE.detach(previewView.getOverlay());
            }
        });
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

    private void logLiveWallpaperPageSelected(int position) {
        switch (position) {
            case 0:
                mUserEventLogger.logLiveWallpaperInfoSelected(
                        mWallpaper.getCollectionId(getActivity()), mWallpaper.getWallpaperId());
                break;
            case 1:
                mUserEventLogger.logLiveWallpaperCustomizeSelected(
                        mWallpaper.getCollectionId(getActivity()), mWallpaper.getWallpaperId());
                break;
        }
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

    private void initInfoPage() {
        View pageInfo = InfoPageController.createView(getLayoutInflater());
        mInfoPageController = new InfoPageController(pageInfo, mPreviewMode);
        mPages.add(Pair.create(getString(R.string.tab_info), pageInfo));
    }

    private void initSettingsPage() {
        final Uri uriSettingsSlice = getSettingsSliceUri(mWallpaper.getWallpaperComponent());
        if (uriSettingsSlice == null) {
            return;
        }

        final View pageSettings = getLayoutInflater().inflate(R.layout.preview_page_settings,
                null /* root */);

        mSettingsSliceView = pageSettings.findViewById(R.id.settings_slice);
        mSettingsSliceView.setMode(SliceView.MODE_LARGE);
        mSettingsSliceView.setScrollable(false);

        // Set LiveData for SliceView
        mSettingsLiveData = SliceLiveData.fromUri(requireContext() /* context */, uriSettingsSlice);
        mSettingsLiveData.observeForever(mSettingsSliceView);

        pageSettings.findViewById(R.id.preview_settings_pane_set_wallpaper_button)
                .setOnClickListener(this::onSetWallpaperClicked);

        mPages.add(Pair.create(getResources().getString(R.string.tab_customize), pageSettings));
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
    protected int getBottomSheetResId() {
        return R.id.bottom_sheet;
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
                        finishActivityWithResultOk();
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable) {
                        showSetWallpaperErrorDialog(destination);
                    }
                });
    }

    @Override
    protected void setBottomSheetContentAlpha(float alpha) {
        mInfoPageController.setContentAlpha(alpha);
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

    private void showDeleteConfirmDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), getDeviceDefaultTheme()))
                .setMessage(R.string.delete_wallpaper_confirmation)
                .setPositiveButton(R.string.delete_live_wallpaper,
                        (dialog, which) -> deleteLiveWallpaper())
                .setNegativeButton(android.R.string.cancel, null /* listener */)
                .create();
        alertDialog.show();
    }

    private void deleteLiveWallpaper() {
        if (mDeleteIntent != null) {
            requireContext().startService(mDeleteIntent);
            finishActivityWithResultOk();
        }
    }

    private boolean isPackagePreInstalled(ApplicationInfo info) {
        if (info != null && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            return true;
        }
        return false;
    }
}
