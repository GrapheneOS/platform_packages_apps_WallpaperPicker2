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

import static android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT;

import android.Manifest.permission;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.wallpaper.WallpaperService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.wallpaper.R;
import com.android.wallpaper.model.ImageWallpaperInfo;
import com.android.wallpaper.model.LiveWallpaperInfo;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.module.UserEventLogger;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

/**
 * Activity that displays a preview of a specific wallpaper and provides the ability to set the
 * wallpaper as the user's current wallpaper. It's "standalone" meaning it doesn't reside in the
 * app navigation hierarchy and can be launched directly via an explicit intent.
 */
public class StandalonePreviewActivity extends BasePreviewActivity {
    private static final String TAG = "StandalonePreview";
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 1;

    private UserEventLogger mUserEventLogger;
    private boolean mIsLivePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        mUserEventLogger = InjectorProvider.getInjector().getUserEventLogger(getApplicationContext());
        mUserEventLogger.logStandalonePreviewLaunched();

        Intent intent = getIntent();
        Uri imageUri = intent.getData();
        Parcelable parcelable = intent.getParcelableExtra(EXTRA_LIVE_WALLPAPER_COMPONENT);
        boolean isStaticPreview = (imageUri != null);
        boolean isLivePreview = parcelable != null && (parcelable instanceof ComponentName);

        if (!isStaticPreview && !isLivePreview) {
            Log.e(TAG,
                    "Neither URI nor LIVE_WALLPAPER_COMPONENT passed in intent; exiting preview");
            finish();
            return;
        }

        if (isStaticPreview) {
            // Check if READ_EXTERNAL_STORAGE permission is needed because the app invoking this
            // activity passed a file:// URI or a content:// URI without a flag to grant read
            // permission.
            boolean isReadPermissionGrantedForImageUri = isReadPermissionGrantedForImageUri(
                    imageUri);
            mUserEventLogger.logStandalonePreviewImageUriHasReadPermission(
                    isReadPermissionGrantedForImageUri);

            // Request storage permission if necessary (i.e., on Android M and later if storage
            // permission has not already been granted) and delay loading the PreviewFragment until
            // the permission is granted.
            if (!isReadPermissionGrantedForImageUri
                    && !isReadExternalStoragePermissionGrantedForApp()) {
                requestPermissions(
                        new String[]{permission.READ_EXTERNAL_STORAGE},
                        READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
            }
        } else if (isLivePreview) {
            mIsLivePreview = true;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            loadPreviewFragment();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Load the preview fragment if the storage permission was granted.
        if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            boolean isGranted = permissions.length > 0
                    && permissions[0].equals(permission.READ_EXTERNAL_STORAGE)
                    && grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            mUserEventLogger.logStandalonePreviewStorageDialogApproved(isGranted);

            // Close the activity because we can't open the image without storage permission.
            if (!isGranted) {
                finish();
            }

            loadPreviewFragment();
        }
    }

    /**
     * Creates a new instance of {@link PreviewFragment} and loads the fragment into this activity's
     * fragment container so that it's shown to the user.
     */
    private void loadPreviewFragment() {
        Intent intent = getIntent();

        boolean testingModeEnabled = intent.getBooleanExtra(EXTRA_TESTING_MODE_ENABLED, false);
        WallpaperInfo wallpaper = mIsLivePreview ? getLiveWallpaperInfo(intent)
                :  new ImageWallpaperInfo(intent.getData());
        // Close the activity because we can't get WallpaperInfo.
        if (wallpaper == null) {
            finish();
            return;
        }
        Fragment fragment = InjectorProvider.getInjector().getPreviewFragment(
                /* context */ this,
                wallpaper,
                mIsLivePreview ? PreviewFragment.MODE_VIEW_ONLY
                        : PreviewFragment.MODE_CROP_AND_SET_WALLPAPER,
                /* viewAsHome= */ true,
                testingModeEnabled);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit();
    }

    private LiveWallpaperInfo getLiveWallpaperInfo(Intent intent) {
        android.app.WallpaperInfo info = getWallpaperInfo(
                intent.getParcelableExtra(EXTRA_LIVE_WALLPAPER_COMPONENT));
        if (info == null) {
            return null;
        }
        return new LiveWallpaperInfo(info);
    }

    /**
     * Gets live wallpaper's {@link android.app.WallpaperInfo} by component name.
     */
    private android.app.WallpaperInfo getWallpaperInfo(ComponentName componentName) {
        // Get the information about this component.  Implemented this way
        // to not allow us to direct the caller to a service that is not a
        // live wallpaper.
        Intent queryIntent = new Intent(WallpaperService.SERVICE_INTERFACE);
        queryIntent.setPackage(componentName.getPackageName());
        List<ResolveInfo> list = getPackageManager().queryIntentServices(
                queryIntent, PackageManager.GET_META_DATA);
        if (list == null) {
            return null;
        }

        for (ResolveInfo ri: list) {
            if (ri.serviceInfo.name.equals(componentName.getClassName())) {
                try {
                    return new android.app.WallpaperInfo(this, ri);
                } catch (XmlPullParserException | IOException e) {
                    Log.w(TAG, "Bad wallpaper " + ri.serviceInfo, e);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Returns whether the user has granted READ_EXTERNAL_STORAGE permission to the app.
     */
    private boolean isReadExternalStoragePermissionGrantedForApp() {
        return getPackageManager().checkPermission(permission.READ_EXTERNAL_STORAGE,
                getPackageName()) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Returns whether the provided image Uri is readable without requiring the app to have the user
     * grant READ_EXTERNAL_STORAGE permission.
     */
    private boolean isReadPermissionGrantedForImageUri(Uri imageUri) {
        return checkUriPermission(
                imageUri,
                Binder.getCallingPid(),
                Binder.getCallingUid(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }
}
