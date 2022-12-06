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
package com.android.wallpaper.testing;

import static com.android.wallpaper.picker.PreviewFragment.ARG_FULL_SCREEN;
import static com.android.wallpaper.picker.PreviewFragment.ARG_PREVIEW_MODE;
import static com.android.wallpaper.picker.PreviewFragment.ARG_TESTING_MODE_ENABLED;
import static com.android.wallpaper.picker.PreviewFragment.ARG_VIEW_AS_HOME;
import static com.android.wallpaper.picker.PreviewFragment.ARG_WALLPAPER;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.wallpaper.compat.WallpaperManagerCompat;
import com.android.wallpaper.effects.EffectsController;
import com.android.wallpaper.model.CategoryProvider;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.AlarmManagerWrapper;
import com.android.wallpaper.module.BitmapCropper;
import com.android.wallpaper.module.CurrentWallpaperInfoFactory;
import com.android.wallpaper.module.CustomizationSections;
import com.android.wallpaper.module.DefaultLiveWallpaperInfoFactory;
import com.android.wallpaper.module.DrawableLayerResolver;
import com.android.wallpaper.module.ExploreIntentChecker;
import com.android.wallpaper.module.Injector;
import com.android.wallpaper.module.LiveWallpaperInfoFactory;
import com.android.wallpaper.module.NetworkStatusNotifier;
import com.android.wallpaper.module.PackageStatusNotifier;
import com.android.wallpaper.module.PartnerProvider;
import com.android.wallpaper.module.SystemFeatureChecker;
import com.android.wallpaper.module.UserEventLogger;
import com.android.wallpaper.module.WallpaperPersister;
import com.android.wallpaper.module.WallpaperPreferences;
import com.android.wallpaper.module.WallpaperPreviewFragmentManager;
import com.android.wallpaper.module.WallpaperRefresher;
import com.android.wallpaper.module.WallpaperRotationRefresher;
import com.android.wallpaper.module.WallpaperStatusChecker;
import com.android.wallpaper.monitor.PerformanceMonitor;
import com.android.wallpaper.network.Requester;
import com.android.wallpaper.picker.ImagePreviewFragment;
import com.android.wallpaper.picker.individual.IndividualPickerFragment;
import com.android.wallpaper.util.DisplayUtils;

/**
 * Test implementation of the dependency injector.
 */
public class TestInjector implements Injector {

    private AlarmManagerWrapper mAlarmManagerWrapper;
    private BitmapCropper mBitmapCropper;
    private CategoryProvider mCategoryProvider;
    private CurrentWallpaperInfoFactory mCurrentWallpaperInfoFactory;
    private ExploreIntentChecker mExploreIntentChecker;
    private NetworkStatusNotifier mNetworkStatusNotifier;
    private PartnerProvider mPartnerProvider;
    private PerformanceMonitor mPerformanceMonitor;
    private SystemFeatureChecker mSystemFeatureChecker;
    private UserEventLogger mUserEventLogger;
    private WallpaperManagerCompat mWallpaperManagerCompat;
    private WallpaperPersister mWallpaperPersister;
    private WallpaperPreferences mPrefs;
    private WallpaperPreviewFragmentManager mWallpaperPreviewFragmentManager;
    private WallpaperRefresher mWallpaperRefresher;
    private WallpaperRotationRefresher mWallpaperRotationRefresher;

    @Override
    public AlarmManagerWrapper getAlarmManagerWrapper(Context unused) {
        if (mAlarmManagerWrapper == null) {
            mAlarmManagerWrapper = new TestAlarmManagerWrapper();
        }
        return mAlarmManagerWrapper;
    }

    @Override
    public BitmapCropper getBitmapCropper() {
        if (mBitmapCropper == null) {
            mBitmapCropper = new com.android.wallpaper.testing.TestBitmapCropper();
        }
        return mBitmapCropper;
    }

    @Override
    public CategoryProvider getCategoryProvider(Context context) {
        if (mCategoryProvider == null) {
            mCategoryProvider = new TestCategoryProvider();
        }
        return mCategoryProvider;
    }

    @Override
    public CurrentWallpaperInfoFactory getCurrentWallpaperInfoFactory(Context context) {
        if (mCurrentWallpaperInfoFactory == null) {
            mCurrentWallpaperInfoFactory =
                    new TestCurrentWallpaperInfoFactory(context.getApplicationContext());
        }
        return mCurrentWallpaperInfoFactory;
    }

    @Override
    public CustomizationSections getCustomizationSections() {
        return null;
    }

    @Override
    public Intent getDeepLinkRedirectIntent(Context context, Uri uri) {
        return null;
    }

    @Override
    public DisplayUtils getDisplayUtils(Context context) {
        return new DisplayUtils(context);
    }

    @Override
    public String getDownloadableIntentAction() {
        return null;
    }

    @Override
    public DrawableLayerResolver getDrawableLayerResolver() {
        return null;
    }

    @Nullable
    @Override
    public EffectsController getEffectsController(Context context,
            EffectsController.EffectsServiceListener listener) {
        return null;
    }

    @Override
    public ExploreIntentChecker getExploreIntentChecker(Context unused) {
        if (mExploreIntentChecker == null) {
            mExploreIntentChecker = new TestExploreIntentChecker();
        }
        return mExploreIntentChecker;
    }

    @Override
    public IndividualPickerFragment getIndividualPickerFragment(String collectionId) {
        return IndividualPickerFragment.newInstance(collectionId);
    }

    @Override
    public LiveWallpaperInfoFactory getLiveWallpaperInfoFactory(Context context) {
        return new DefaultLiveWallpaperInfoFactory();
    }

    @Override
    public NetworkStatusNotifier getNetworkStatusNotifier(Context context) {
        if (mNetworkStatusNotifier == null) {
            mNetworkStatusNotifier = new TestNetworkStatusNotifier();
        }
        return mNetworkStatusNotifier;
    }

    @Override
    public PackageStatusNotifier getPackageStatusNotifier(Context context) {
        return null;
    }

    @Override
    public PartnerProvider getPartnerProvider(Context context) {
        if (mPartnerProvider == null) {
            mPartnerProvider = new TestPartnerProvider();
        }
        return mPartnerProvider;
    }

    @Override
    public PerformanceMonitor getPerformanceMonitor() {
        if (mPerformanceMonitor == null) {
            mPerformanceMonitor = new TestPerformanceMonitor();
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
        ImagePreviewFragment fragment = new ImagePreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Requester getRequester(Context unused) {
        return null;
    }

    @Override
    public SystemFeatureChecker getSystemFeatureChecker() {
        if (mSystemFeatureChecker == null) {
            mSystemFeatureChecker = new com.android.wallpaper.testing.TestSystemFeatureChecker();
        }
        return mSystemFeatureChecker;
    }

    @Override
    public UserEventLogger getUserEventLogger(Context unused) {
        if (mUserEventLogger == null) {
            mUserEventLogger = new com.android.wallpaper.testing.TestUserEventLogger();
        }
        return mUserEventLogger;
    }

    @Override
    public WallpaperManagerCompat getWallpaperManagerCompat(Context context) {
        if (mWallpaperManagerCompat == null) {
            mWallpaperManagerCompat = new com.android.wallpaper.testing.TestWallpaperManagerCompat(
                    context.getApplicationContext());
        }
        return mWallpaperManagerCompat;
    }

    @Override
    public WallpaperPersister getWallpaperPersister(Context context) {
        if (mWallpaperPersister == null) {
            mWallpaperPersister = new TestWallpaperPersister(context.getApplicationContext());
        }
        return mWallpaperPersister;
    }

    @Override
    public WallpaperPreferences getPreferences(Context context) {
        if (mPrefs == null) {
            mPrefs = new TestWallpaperPreferences();
        }
        return mPrefs;
    }

    @Override
    public WallpaperPreviewFragmentManager getWallpaperPreviewFragmentManager() {
        if (mWallpaperPreviewFragmentManager == null) {
            mWallpaperPreviewFragmentManager = new TestWallpaperPreviewFragmentManager();
        }
        return mWallpaperPreviewFragmentManager;
    }

    @Override
    public WallpaperRefresher getWallpaperRefresher(Context context) {
        if (mWallpaperRefresher == null) {
            mWallpaperRefresher = new TestWallpaperRefresher(context.getApplicationContext());
        }
        return mWallpaperRefresher;
    }

    @Override
    public WallpaperRotationRefresher getWallpaperRotationRefresher() {
        if (mWallpaperRotationRefresher == null) {
            mWallpaperRotationRefresher = (context, listener) -> {
                // Not implemented
                listener.onError();
            };
        }
        return mWallpaperRotationRefresher;
    }

    @Override
    public WallpaperStatusChecker getWallpaperStatusChecker() {
        return new WallpaperStatusChecker() {
            @Override
            public boolean isHomeStaticWallpaperSet(Context context) {
                return true;
            }

            @Override
            public boolean isLockWallpaperSet(Context context) {
                return true;
            }
        };
    }
}
