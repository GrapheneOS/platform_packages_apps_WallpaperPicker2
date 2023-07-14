/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.app.WallpaperManager.FLAG_LOCK;
import static android.app.WallpaperManager.FLAG_SYSTEM;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.WallpaperManager;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.android.wallpaper.testing.TestBitmapCropper;
import com.android.wallpaper.testing.TestWallpaperManagerCompat;
import com.android.wallpaper.testing.TestWallpaperPreferences;
import com.android.wallpaper.testing.TestWallpaperStatusChecker;
import com.android.wallpaper.util.DisplayUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DefaultWallpaperPersisterTest {

    private DefaultWallpaperPersister mPersister;
    private WallpaperManager mManager;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mManager = spy(WallpaperManager.getInstance(context));
        TestWallpaperManagerCompat managerCompat = new TestWallpaperManagerCompat(context);
        TestWallpaperPreferences prefs = new TestWallpaperPreferences();
        WallpaperChangedNotifier changedNotifier = spy(WallpaperChangedNotifier.getInstance());
        DisplayUtils displayUtils = new DisplayUtils(context);
        TestBitmapCropper cropper = new TestBitmapCropper();
        TestWallpaperStatusChecker statusChecker = new TestWallpaperStatusChecker();

        mPersister = new DefaultWallpaperPersister(context, mManager, managerCompat, prefs,
                changedNotifier, displayUtils, cropper, statusChecker);
    }

    @Test
    public void isSeparateLockScreenWallpaperSet_trueIfSet() {
        doReturn(-1).when(mManager).getWallpaperId(FLAG_LOCK);

        assertThat(mPersister.getDefaultWhichWallpaper()).isEqualTo(FLAG_SYSTEM | FLAG_LOCK);
    }

    @Test
    public void isSeparateLockScreenWallpaperSet_falseIfUnset() {
        doReturn(1).when(mManager).getWallpaperId(FLAG_LOCK);

        assertThat(mPersister.getDefaultWhichWallpaper()).isEqualTo(FLAG_SYSTEM);
    }
}
