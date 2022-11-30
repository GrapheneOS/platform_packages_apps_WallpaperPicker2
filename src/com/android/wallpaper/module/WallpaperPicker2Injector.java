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
package com.android.wallpaper.module;

import static com.android.wallpaper.picker.PreviewFragment.ARG_FULL_SCREEN;
import static com.android.wallpaper.picker.PreviewFragment.ARG_PREVIEW_MODE;
import static com.android.wallpaper.picker.PreviewFragment.ARG_TESTING_MODE_ENABLED;
import static com.android.wallpaper.picker.PreviewFragment.ARG_VIEW_AS_HOME;
import static com.android.wallpaper.picker.PreviewFragment.ARG_WALLPAPER;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.android.wallpaper.compat.WallpaperManagerCompat;
import com.android.wallpaper.effects.EffectsController;
import com.android.wallpaper.model.CategoryProvider;
import com.android.wallpaper.model.LiveWallpaperInfo;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.monitor.PerformanceMonitor;
import com.android.wallpaper.network.Requester;
import com.android.wallpaper.network.WallpaperRequester;
import com.android.wallpaper.picker.CustomizationPickerActivity;
import com.android.wallpaper.picker.ImagePreviewFragment;
import com.android.wallpaper.picker.LivePreviewFragment;
import com.android.wallpaper.picker.PreviewFragment;
import com.android.wallpaper.picker.individual.IndividualPickerFragment;
import com.android.wallpaper.util.DisplayUtils;

/**
 * A concrete, real implementation of the dependency provider.
 */
public class WallpaperPicker2Injector implements Injector {
    private AlarmManagerWrapper mAlarmManagerWrapper;
    private BitmapCropper mBitmapCropper;
    private CategoryProvider mCategoryProvider;
    private CurrentWallpaperInfoFactory mCurrentWallpaperFactory;
    private CustomizationSections mCustomizationSections;
    private DisplayUtils mDisplayUtils;
    private DrawableLayerResolver mDrawableLayerResolver;
    private ExploreIntentChecker mExploreIntentChecker;
    private LiveWallpaperInfoFactory mLiveWallpaperInfoFactory;
    private NetworkStatusNotifier mNetworkStatusNotifier;
    private PackageStatusNotifier mPackageStatusNotifier;
    private PartnerProvider mPartnerProvider;
    private PerformanceMonitor mPerformanceMonitor;
    private Requester mRequester;
    private SystemFeatureChecker mSystemFeatureChecker;
    private UserEventLogger mUserEventLogger;
    private WallpaperManagerCompat mWallpaperManagerCompat;
    private WallpaperPersister mWallpaperPersister;
    private WallpaperPreferences mPrefs;
    private WallpaperPreviewFragmentManager mWallpaperPreviewFragmentManager;
    private WallpaperRefresher mWallpaperRefresher;
    private WallpaperRotationRefresher mWallpaperRotationRefresher;
    private WallpaperStatusChecker mWallpaperStatusChecker;

    @Override
    public synchronized AlarmManagerWrapper getAlarmManagerWrapper(Context context) {
        if (mAlarmManagerWrapper == null) {
            mAlarmManagerWrapper = new DefaultAlarmManagerWrapper(context.getApplicationContext());
        }
        return mAlarmManagerWrapper;
    }

    @Override
    public synchronized BitmapCropper getBitmapCropper() {
        if (mBitmapCropper == null) {
            mBitmapCropper = new DefaultBitmapCropper();
        }
        return mBitmapCropper;
    }

    @Override
    public CategoryProvider getCategoryProvider(Context context) {
        if (mCategoryProvider == null) {
            mCategoryProvider = new DefaultCategoryProvider(context.getApplicationContext());
        }
        return mCategoryProvider;
    }

    @Override
    public synchronized CurrentWallpaperInfoFactory getCurrentWallpaperInfoFactory(
            Context context) {
        if (mCurrentWallpaperFactory == null) {
            mCurrentWallpaperFactory =
                    new DefaultCurrentWallpaperInfoFactory(context.getApplicationContext());
        }
        return mCurrentWallpaperFactory;
    }

    @Override
    public CustomizationSections getCustomizationSections() {
        if (mCustomizationSections == null) {
            mCustomizationSections = new WallpaperPickerSections();
        }
        return mCustomizationSections;
    }

    @Override
    public Intent getDeepLinkRedirectIntent(Context context, Uri uri) {
        Intent intent = new Intent();
        intent.setClass(context, CustomizationPickerActivity.class);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    @Override
    public DisplayUtils getDisplayUtils(Context context) {
        if (mDisplayUtils == null) {
            mDisplayUtils = new DisplayUtils(context.getApplicationContext());
        }
        return mDisplayUtils;
    }

    @Override
    public String getDownloadableIntentAction() {
        return null;
    }

    @Override
    public DrawableLayerResolver getDrawableLayerResolver() {
        if (mDrawableLayerResolver == null) {
            mDrawableLayerResolver = new DefaultDrawableLayerResolver();
        }
        return mDrawableLayerResolver;
    }

    @Override
    public EffectsController getEffectsController(Context context,
            EffectsController.EffectsServiceListener listener) {
        return null;
    }

    @Override
    public synchronized ExploreIntentChecker getExploreIntentChecker(Context context) {
        if (mExploreIntentChecker == null) {
            mExploreIntentChecker = new DefaultExploreIntentChecker(
                    context.getApplicationContext());
        }
        return mExploreIntentChecker;
    }

    @Override
    public synchronized Fragment getIndividualPickerFragment(String collectionId) {
        return IndividualPickerFragment.newInstance(collectionId);
    }

    @Override
    public LiveWallpaperInfoFactory getLiveWallpaperInfoFactory(Context context) {
        if (mLiveWallpaperInfoFactory == null) {
            mLiveWallpaperInfoFactory = new DefaultLiveWallpaperInfoFactory();
        }
        return mLiveWallpaperInfoFactory;
    }

    @Override
    public synchronized NetworkStatusNotifier getNetworkStatusNotifier(Context context) {
        if (mNetworkStatusNotifier == null) {
            mNetworkStatusNotifier = new DefaultNetworkStatusNotifier(
                    context.getApplicationContext());
        }
        return mNetworkStatusNotifier;
    }

    @Override
    public synchronized PackageStatusNotifier getPackageStatusNotifier(Context context) {
        if (mPackageStatusNotifier == null) {
            mPackageStatusNotifier = new DefaultPackageStatusNotifier(
                    context.getApplicationContext());
        }
        return mPackageStatusNotifier;
    }

    @Override
    public synchronized PartnerProvider getPartnerProvider(Context context) {
        if (mPartnerProvider == null) {
            mPartnerProvider = new DefaultPartnerProvider(context.getApplicationContext());
        }
        return mPartnerProvider;
    }

    @Override
    public synchronized PerformanceMonitor getPerformanceMonitor() {
        if (mPerformanceMonitor == null) {
            mPerformanceMonitor = new PerformanceMonitor() {
                @Override
                public void recordFullResPreviewLoadedMemorySnapshot() {
                    // No Op
                }
            };
        }
        return mPerformanceMonitor;
    }

    @Override
    public Fragment getPreviewFragment(Context context, WallpaperInfo wallpaperInfo, int mode,
            boolean viewAsHome, boolean viewFullScreen, boolean testingModeEnabled) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_WALLPAPER, wallpaperInfo);
        args.putInt(ARG_PREVIEW_MODE, mode);
        args.putBoolean(ARG_VIEW_AS_HOME, viewAsHome);
        args.putBoolean(ARG_FULL_SCREEN, viewFullScreen);
        args.putBoolean(ARG_TESTING_MODE_ENABLED, testingModeEnabled);
        PreviewFragment fragment = wallpaperInfo instanceof LiveWallpaperInfo
                ? new LivePreviewFragment() : new ImagePreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public synchronized Requester getRequester(Context context) {
        if (mRequester == null) {
            mRequester = new WallpaperRequester(context.getApplicationContext());
        }
        return mRequester;
    }

    @Override
    public synchronized SystemFeatureChecker getSystemFeatureChecker() {
        if (mSystemFeatureChecker == null) {
            mSystemFeatureChecker = new DefaultSystemFeatureChecker();
        }
        return mSystemFeatureChecker;
    }

    @Override
    public UserEventLogger getUserEventLogger(Context context) {
        if (mUserEventLogger == null) {
            mUserEventLogger = new NoOpUserEventLogger();
        }
        return mUserEventLogger;
    }

    @Override
    public synchronized WallpaperManagerCompat getWallpaperManagerCompat(Context context) {
        if (mWallpaperManagerCompat == null) {
            mWallpaperManagerCompat = WallpaperManagerCompat.getInstance(context);
        }
        return mWallpaperManagerCompat;
    }

    @Override
    public synchronized WallpaperPersister getWallpaperPersister(Context context) {
        if (mWallpaperPersister == null) {
            mWallpaperPersister = new DefaultWallpaperPersister(context.getApplicationContext());
        }
        return mWallpaperPersister;
    }

    @Override
    public synchronized WallpaperPreferences getPreferences(Context context) {
        if (mPrefs == null) {
            mPrefs = new DefaultWallpaperPreferences(context.getApplicationContext());
        }
        return mPrefs;
    }

    @Override
    public synchronized WallpaperPreviewFragmentManager getWallpaperPreviewFragmentManager() {
        if (mWallpaperPreviewFragmentManager == null) {
            mWallpaperPreviewFragmentManager = new DefaultWallpaperPreviewFragmentManager();
        }
        return mWallpaperPreviewFragmentManager;
    }

    @Override
    public synchronized WallpaperRefresher getWallpaperRefresher(Context context) {
        if (mWallpaperRefresher == null) {
            mWallpaperRefresher = new DefaultWallpaperRefresher(context.getApplicationContext());
        }
        return mWallpaperRefresher;
    }

    @Override
    public synchronized WallpaperRotationRefresher getWallpaperRotationRefresher() {
        if (mWallpaperRotationRefresher == null) {
            mWallpaperRotationRefresher = new WallpaperRotationRefresher() {
                @Override
                public void refreshWallpaper(Context context, Listener listener) {
                    // Not implemented
                    listener.onError();
                }
            };
        }
        return mWallpaperRotationRefresher;
    }

    @Override
    public WallpaperStatusChecker getWallpaperStatusChecker() {
        if (mWallpaperStatusChecker == null) {
            mWallpaperStatusChecker = new DefaultWallpaperStatusChecker();
        }
        return mWallpaperStatusChecker;
    }
}
