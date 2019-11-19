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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.service.wallpaper.WallpaperService;
import android.service.wallpaper.WallpaperSettingsActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.slice.Slice;
import androidx.slice.widget.SliceLiveData;
import androidx.slice.widget.SliceView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.wallpaper.R;
import com.android.wallpaper.compat.BuildCompat;
import com.android.wallpaper.model.LiveWallpaperInfo;
import com.android.wallpaper.module.WallpaperPersister.SetWallpaperCallback;
import com.android.wallpaper.widget.MaterialProgressDrawable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment which displays the UI for previewing an individual live wallpaper, its attribution
 * information and settings slices if available.
 */
public class LivePreviewFragment extends PreviewFragment {

    private static final String TAG = "LivePreviewFragment";

    private static final String KEY_ACTION_DELETE_LIVE_WALLPAPER = "action_delete_live_wallpaper";
    private static final String EXTRA_LIVE_WALLPAPER_INFO = "android.live_wallpaper.info";

    /**
     * Instance of {@link WallpaperConnection} used to bind to the live wallpaper service to show
     * it in this preview fragment.
     * @see IWallpaperConnection
     */
    private WallpaperConnection mWallpaperConnection;

    private Intent mWallpaperIntent;
    private Intent mDeleteIntent;
    private Intent mSettingsIntent;

    private List<Pair<String, View>> mPages;
    private ImageView mLoadingIndicator;
    private TextView mAttributionTitle;
    private TextView mAttributionSubtitle1;
    private TextView mAttributionSubtitle2;
    private Button mExploreButton;
    private Button mSetWallpaperButton;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private SliceView mSettingsSliceView;
    private LiveData<Slice> mSettingsLiveData;
    private View mSpacer;
    private View mLoadingScrim;
    private MaterialProgressDrawable mProgressDrawable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.app.WallpaperInfo info = mWallpaper.getWallpaperComponent();
        mWallpaperIntent = new Intent(WallpaperService.SERVICE_INTERFACE)
                .setClassName(info.getPackageName(), info.getServiceName());
        setUpExploreIntent(null);

        android.app.WallpaperInfo currentWallpaper =
                WallpaperManager.getInstance(requireContext()).getWallpaperInfo();
        String deleteAction = getDeleteAction(info.getServiceInfo(),
                (currentWallpaper == null) ? null : currentWallpaper.getServiceInfo());

        if (!TextUtils.isEmpty(deleteAction)) {
            mDeleteIntent = new Intent(deleteAction);
            mDeleteIntent.setPackage(info.getPackageName());
            mDeleteIntent.putExtra(EXTRA_LIVE_WALLPAPER_INFO, info);
        }

        String settingsActivity = info.getSettingsActivity();
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
        mLoadingIndicator = view.findViewById(R.id.loading_indicator);
        setUpLoadingIndicator();

        mWallpaperConnection = new WallpaperConnection(mWallpaperIntent, activity);
        container.post(() -> {
            if (!mWallpaperConnection.connect()) {
                mWallpaperConnection = null;
            }
        });

        return view;
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
        super.onDestroy();
    }

    @Override
    protected void setUpBottomSheetView(ViewGroup bottomSheet) {

        initInfoPage();
        initSettingsPage();

        mViewPager = bottomSheet.findViewById(R.id.viewpager);
        mTabLayout = bottomSheet.findViewById(R.id.tablayout);

        // Create PagerAdapter
        final PagerAdapter pagerAdapter = new PagerAdapter() {
            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                final View page = mPages.get(position).second;
                container.addView(page);
                return page;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position,
                    @NonNull Object object) {
                if (object instanceof View) {
                    container.removeView((View) object);
                }
            }

            @Override
            public int getCount() {
                return mPages.size();
            }

            @Override
            public CharSequence getPageTitle(int position) {
                try {
                    return mPages.get(position).first;
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return (view == object);
            }
        };

        // Add OnPageChangeListener to re-measure ViewPager's height
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mViewPager.requestLayout();
            }
        });

        // Set PagerAdapter
        mViewPager.setAdapter(pagerAdapter);

        // Make TabLayout visible if there are more than one page
        if (mPages.size() > 1) {
            mTabLayout.setVisibility(View.VISIBLE);
            mTabLayout.setupWithViewPager(mViewPager);
        }
        mViewPager.setCurrentItem(0);
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

        // We don't want to show the spinner every time we load a wallpaper if it loads quickly;
        // instead, only start showing the spinner after 100 ms
        mLoadingIndicator.postDelayed(() -> {
            if ((mWallpaperConnection == null || !mWallpaperConnection.isEngineReady())
                    && !mTestingModeEnabled) {
                mLoadingIndicator.setVisibility(View.VISIBLE);
                mLoadingIndicator.setAlpha(1f);
                if (mProgressDrawable != null) {
                    mProgressDrawable.start();
                }
            }
        }, 100);
    }

    private void initInfoPage() {
        View pageInfo = getLayoutInflater().inflate(R.layout.preview_page_info, null /* root */);

        mAttributionTitle = pageInfo.findViewById(R.id.preview_attribution_pane_title);
        mAttributionSubtitle1 = pageInfo.findViewById(R.id.preview_attribution_pane_subtitle1);
        mAttributionSubtitle2 = pageInfo.findViewById(R.id.preview_attribution_pane_subtitle2);
        mSpacer = pageInfo.findViewById(R.id.spacer);

        mExploreButton = pageInfo.findViewById(R.id.preview_attribution_pane_explore_button);
        mSetWallpaperButton = pageInfo.findViewById(
                R.id.preview_attribution_pane_set_wallpaper_button);

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

        mPages.add(Pair.create(getResources().getString(R.string.tab_customize), pageSettings));
    }

    private void populateAttributionPane() {
        final Context context = getContext();

        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);

        List<String> attributions = mWallpaper.getAttributions(context);
        if (attributions.size() > 0 && attributions.get(0) != null) {
            mAttributionTitle.setText(attributions.get(0));
        }

        if (mWallpaper.getWallpaperComponent().getShowMetadataInPreview()) {

            if (attributions.size() > 1 && attributions.get(1) != null) {
                mAttributionSubtitle1.setVisibility(View.VISIBLE);
                mAttributionSubtitle1.setText(attributions.get(1));
            }

            if (attributions.size() > 2 && attributions.get(2) != null) {
                mAttributionSubtitle2.setVisibility(View.VISIBLE);
                mAttributionSubtitle2.setText(attributions.get(2));
            }

        } else {
            mExploreIntent = null;
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
        if (mBottomSheetInitialState == STATE_EXPANDED) {
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

        bottomSheetBehavior.setState(mBottomSheetInitialState);
    }

    @SuppressLint("NewApi") //Already checking with isAtLeastQ
    private Uri getSettingsSliceUri(android.app.WallpaperInfo info) {
        if (BuildCompat.isAtLeastQ()) {
            return info.getSettingsSliceUri();
        }
        return null;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_live_preview;
    }

    @Override
    protected int getBottomSheetResId() {
        return R.id.bottom_sheet;
    }

    @Override
    protected void setCurrentWallpaper(int destination) {
        mWallpaperSetter.setCurrentWallpaper(getActivity(), mWallpaper, null,
                destination, 0, null, new SetWallpaperCallback() {
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

    @Override
    protected void setBottomSheetContentAlpha(float alpha) {
        mExploreButton.setAlpha(alpha);
        mAttributionTitle.setAlpha(alpha);
        mAttributionSubtitle1.setAlpha(alpha);
        mAttributionSubtitle2.setAlpha(alpha);
    }

    @Override
    protected void setUpExploreButton(Button exploreButton) {
        super.setUpExploreButton(exploreButton);
        if (exploreButton.getVisibility() != View.VISIBLE) {
            return;
        }
        Context context = requireContext();
        CharSequence exploreLabel = ((LiveWallpaperInfo) mWallpaper).getActionDescription(context);
        if (!TextUtils.isEmpty(exploreLabel)) {
            exploreButton.setText(exploreLabel);
        }
    }

    @Nullable
    private String getDeleteAction(ServiceInfo serviceInfo,
            @Nullable ServiceInfo currentService) {
        if (!isPackagePreInstalled(serviceInfo.applicationInfo)) {
            Log.d(TAG, "This wallpaper is not pre-installed: " + serviceInfo.name);
            return null;
        }

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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.configure).setVisible(mSettingsIntent != null);
        menu.findItem(R.id.delete_wallpaper).setVisible(mDeleteIntent != null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.configure) {
            if (getActivity() != null) {
                startActivity(mSettingsIntent);
                return true;
            }
        } else if (id == R.id.delete_wallpaper) {
            showDeleteConfirmDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    private class WallpaperConnection extends IWallpaperConnection.Stub
            implements ServiceConnection {
        private final Activity mActivity;
        private final Intent mIntent;
        private IWallpaperService mService;
        private IWallpaperEngine mEngine;
        private boolean mConnected;
        private boolean mIsVisible;
        private boolean mIsEngineVisible;
        private boolean mEngineReady;

        WallpaperConnection(Intent intent, Activity activity) {
            mActivity = activity;
            mIntent = intent;
        }

        public boolean connect() {
            synchronized (this) {
                if (!mActivity.bindService(mIntent, this, Context.BIND_AUTO_CREATE)) {
                    return false;
                }

                mConnected = true;
                return true;
            }
        }

        public void disconnect() {
            synchronized (this) {
                mConnected = false;
                if (mEngine != null) {
                    try {
                        mEngine.destroy();
                    } catch (RemoteException e) {
                        // Ignore
                    }
                    mEngine = null;
                }
                try {
                    mActivity.unbindService(this);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Can't unbind wallpaper service. "
                            + "It might have crashed, just ignoring.", e);
                }
                mService = null;
            }
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (mWallpaperConnection == this) {
                mService = IWallpaperService.Stub.asInterface(service);
                try {
                    View root = mActivity.getWindow().getDecorView();
                    int displayId = root.getDisplay().getDisplayId();
                    mService.attach(this, root.getWindowToken(),
                            LayoutParams.TYPE_APPLICATION_MEDIA,
                            true, root.getWidth(), root.getHeight(),
                            new Rect(0, 0, 0, 0), displayId);
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed attaching wallpaper; clearing", e);
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mEngine = null;
            if (mWallpaperConnection == this) {
                Log.w(TAG, "Wallpaper service gone: " + name);
            }
        }

        public void attachEngine(IWallpaperEngine engine, int displayId) {
            synchronized (this) {
                if (mConnected) {
                    mEngine = engine;
                    if (mIsVisible) {
                        setEngineVisibility(true);
                    }
                } else {
                    try {
                        engine.destroy();
                    } catch (RemoteException e) {
                        // Ignore
                    }
                }
            }
        }

        public ParcelFileDescriptor setWallpaper(String name) {
            return null;
        }

        @Override
        public void onWallpaperColorsChanged(WallpaperColors colors, int displayId)
                throws RemoteException {

        }

        @Override
        public void engineShown(IWallpaperEngine engine)  {
            mLoadingScrim.post(() -> {
                mLoadingScrim.animate()
                        .alpha(0f)
                        .setDuration(220)
                        .setStartDelay(300)
                        .setInterpolator(AnimationUtils.loadInterpolator(mActivity,
                                android.R.interpolator.fast_out_linear_in))
                        .withEndAction(() -> {
                            if (mLoadingIndicator != null) {
                                mLoadingIndicator.setVisibility(View.GONE);
                            }
                            if (mProgressDrawable != null) {
                                mProgressDrawable.stop();
                            }
                            mLoadingScrim.setVisibility(View.INVISIBLE);
                            populateAttributionPane();
                        });
            });
            mEngineReady = true;
        }

        public boolean isEngineReady() {
            return mEngineReady;
        }

        public void setVisibility(boolean visible) {
            mIsVisible = visible;
            setEngineVisibility(visible);
        }

        private void setEngineVisibility(boolean visible) {
            if (mEngine != null && visible != mIsEngineVisible) {
                try {
                    mEngine.setVisibility(visible);
                    mIsEngineVisible = visible;
                } catch (RemoteException e) {
                    Log.w(TAG, "Failure setting wallpaper visibility ", e);
                }
            }
        }
    }
}
