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
package com.android.wallpaper.picker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.wallpaper.R;
import com.android.wallpaper.config.BaseFlags;
import com.android.wallpaper.model.InlinePreviewIntentFactory;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.module.LargeScreenMultiPanesChecker;
import com.android.wallpaper.picker.AppbarFragment.AppbarFragmentHost;
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity;
import com.android.wallpaper.util.ActivityUtils;

/**
 * Activity that displays a view-only preview of a specific wallpaper.
 */
public class ViewOnlyPreviewActivity extends BasePreviewActivity implements AppbarFragmentHost {

    /**
     * Returns a new Intent with the provided WallpaperInfo instance put as an extra.
     */
    public static Intent newIntent(Context context, WallpaperInfo wallpaper) {
        return new Intent(context, ViewOnlyPreviewActivity.class)
                .putExtra(EXTRA_WALLPAPER_INFO, wallpaper);
    }

    protected static Intent newIntent(Context context, WallpaperInfo wallpaper,
            boolean isVewAsHome, boolean isAssetIdPresent) {
        return newIntent(context, wallpaper).putExtra(EXTRA_VIEW_AS_HOME, isVewAsHome)
                        .putExtra(IS_ASSET_ID_PRESENT, isAssetIdPresent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        enableFullScreen();

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            Intent intent = getIntent();
            WallpaperInfo wallpaper = intent.getParcelableExtra(EXTRA_WALLPAPER_INFO);
            boolean testingModeEnabled = intent.getBooleanExtra(EXTRA_TESTING_MODE_ENABLED, false);
            boolean viewAsHome = intent.getBooleanExtra(EXTRA_VIEW_AS_HOME, true);
            boolean isAssetIdPresent = intent.getBooleanExtra(IS_ASSET_ID_PRESENT, true);
            fragment = InjectorProvider.getInjector().getPreviewFragment(
                    /* context */ this,
                    wallpaper,
                    PreviewFragment.MODE_VIEW_ONLY,
                    viewAsHome,
                    /* viewFullScreen= */ false,
                    testingModeEnabled, isAssetIdPresent);
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public void onUpArrowPressed() {
        onBackPressed();
    }

    @Override
    public boolean isUpArrowSupported() {
        return !ActivityUtils.isSUWMode(getBaseContext());
    }

    /**
     * Implementation that provides an intent to start a PreviewActivity.
     */
    public static class ViewOnlyPreviewActivityIntentFactory implements InlinePreviewIntentFactory {
        private boolean mIsHomeAndLockPreviews;
        private boolean mIsViewAsHome;

        @Override
        public Intent newIntent(Context context, WallpaperInfo wallpaper,
                boolean isAssetIdPresent) {
            LargeScreenMultiPanesChecker multiPanesChecker = new LargeScreenMultiPanesChecker();
            final boolean isMultiPanel = multiPanesChecker.isMultiPanesEnabled(context);
            final BaseFlags flags = InjectorProvider.getInjector().getFlags();
            if (flags.isMultiCropPreviewUiEnabled() && flags.isMultiCropEnabled()) {
                return WallpaperPreviewActivity.Companion.newIntent(context,
                        wallpaper, /* isNewTask= */ isMultiPanel);
            }

            // Launch a full preview activity for devices supporting multipanel mode
            if (isMultiPanel) {
                return FullPreviewActivity.newIntent(context, wallpaper, mIsViewAsHome,
                        isAssetIdPresent);
            }

            if (mIsHomeAndLockPreviews) {
                return ViewOnlyPreviewActivity.newIntent(context, wallpaper, mIsViewAsHome,
                        isAssetIdPresent);
            }
            return ViewOnlyPreviewActivity.newIntent(context, wallpaper);
        }

        protected void setAsHomePreview(boolean isHomeAndLockPreview, boolean isViewAsHome) {
            mIsHomeAndLockPreviews = isHomeAndLockPreview;
            mIsViewAsHome = isViewAsHome;
        }
    }
}
