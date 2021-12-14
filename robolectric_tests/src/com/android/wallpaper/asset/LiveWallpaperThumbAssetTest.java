/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.wallpaper.asset;

import android.app.WallpaperInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;

import androidx.fragment.app.FragmentActivity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link LiveWallpaperThumbAsset}.
 */
@RunWith(RobolectricTestRunner.class)
public class LiveWallpaperThumbAssetTest {

    private TestLiveWallpaperThumbAsset mLiveWallpaperThumbAsset;

    /**
     * Setup of the test.
     */
    @Before
    public void setUp() {
        Context context = Robolectric.buildActivity(FragmentActivity.class).get();
        mLiveWallpaperThumbAsset = new TestLiveWallpaperThumbAsset(context, null);
    }

    /**
     * Checks that the function of freeDrawable work fine.
     */
    @Test
    public void testFreeDrawable() {
        mLiveWallpaperThumbAsset.release();
        Assert.assertEquals(mLiveWallpaperThumbAsset.mThumbnailDrawable, null);
    }

    private static final class TestLiveWallpaperThumbAsset extends LiveWallpaperThumbAsset {
        /**
         * Mock of {@link LiveWallpaperThumbAsset}.
         */
        TestLiveWallpaperThumbAsset(Context context, WallpaperInfo info) {
            super(context, null);
            Bitmap mBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            mBitmap.setPixel(0, 0, Color.TRANSPARENT);
            mThumbnailDrawable = new BitmapDrawable(mBitmap);
        }
    }
}
