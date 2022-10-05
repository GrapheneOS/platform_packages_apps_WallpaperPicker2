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

import com.android.wallpaper.model.CategoryProvider;
import com.android.wallpaper.model.LiveWallpaperInfo;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.monitor.PerformanceMonitor;
import com.android.wallpaper.picker.CustomizationPickerActivity;
import com.android.wallpaper.picker.ImagePreviewFragment;
import com.android.wallpaper.picker.LivePreviewFragment;
import com.android.wallpaper.picker.PreviewFragment;

/**
 * A concrete, real implementation of the dependency provider.
 */
public class WallpapersInjector extends BaseWallpaperInjector {
    private CategoryProvider mCategoryProvider;
    private UserEventLogger mUserEventLogger;
    private WallpaperRotationRefresher mWallpaperRotationRefresher;
    private PerformanceMonitor mPerformanceMonitor;

    @Override
    public synchronized CategoryProvider getCategoryProvider(Context context) {
        if (mCategoryProvider == null) {
            mCategoryProvider = new DefaultCategoryProvider(context.getApplicationContext());
        }
        return mCategoryProvider;
    }

    @Override
    public synchronized UserEventLogger getUserEventLogger(Context context) {
        if (mUserEventLogger == null) {
            mUserEventLogger = new NoOpUserEventLogger();
        }
        return mUserEventLogger;
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
    public Fragment getPreviewFragment(
            Context context,
            WallpaperInfo wallpaperInfo,
            int mode,
            boolean viewAsHome,
            boolean viewFullScreen,
            boolean testingModeEnabled) {
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
    public Intent getDeepLinkRedirectIntent(Context context, Uri uri) {
        Intent intent = new Intent();
        intent.setClass(context, CustomizationPickerActivity.class);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
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
    public synchronized LoggingOptInStatusProvider getLoggingOptInStatusProvider(Context context) {
        return null;
    }

    @Override
    public String getDownloadableIntentAction() {
        return null;
    }
}
