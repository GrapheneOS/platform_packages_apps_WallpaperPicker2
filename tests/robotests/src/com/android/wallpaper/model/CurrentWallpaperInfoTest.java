/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.model;

import static com.google.common.truth.Truth.assertThat;

import android.app.WallpaperManager;
import android.app.WallpaperManager.SetWallpaperFlags;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class CurrentWallpaperInfoTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void parcelRoundTrip_succeeds() {
        final List<String> attributions = List.of("one", "two");
        final String actionUrl = "http://www.bogus.com";
        final String collectionId = "collection1";
        final @SetWallpaperFlags int flag = WallpaperManager.FLAG_SYSTEM;
        final Uri imageWallpaperUri = Uri.parse("content://my/content");
        CurrentWallpaperInfo saved = new CurrentWallpaperInfo(attributions, actionUrl, collectionId,
                flag, imageWallpaperUri);

        Parcel parcel = Parcel.obtain();
        saved.writeToParcel(parcel, 0);
        // Reset parcel for reading
        parcel.setDataPosition(0);
        CurrentWallpaperInfo restored = CurrentWallpaperInfo.CREATOR.createFromParcel(parcel);

        assertThat(restored.getAttributions(mContext)).containsExactlyElementsIn(
                attributions).inOrder();
        assertThat(restored.getActionUrl(mContext)).isEqualTo(actionUrl);
        assertThat(restored.getCollectionId(mContext)).isEqualTo(collectionId);
        assertThat(restored.getWallpaperManagerFlag()).isEqualTo(flag);
        assertThat(restored.getImageWallpaperUri()).isEqualTo(imageWallpaperUri);
    }

    @Test
    // Same as above test, but with all nullable fields set to null
    public void parcelRoundTrip_withNulls_succeeds() {
        final List<String> attributions = List.of("one", "two");
        final String actionUrl = "http://www.bogus.com";
        final String collectionId = "collection1";
        final @SetWallpaperFlags int flag = WallpaperManager.FLAG_SYSTEM;
        final Uri imageWallpaperUri = null;
        CurrentWallpaperInfo saved = new CurrentWallpaperInfo(attributions, actionUrl, collectionId,
                flag, imageWallpaperUri);

        Parcel parcel = Parcel.obtain();
        saved.writeToParcel(parcel, 0);
        // Reset parcel for reading
        parcel.setDataPosition(0);
        CurrentWallpaperInfo restored = CurrentWallpaperInfo.CREATOR.createFromParcel(parcel);

        assertThat(restored.getAttributions(mContext)).containsExactlyElementsIn(
                attributions).inOrder();
        assertThat(restored.getActionUrl(mContext)).isEqualTo(actionUrl);
        assertThat(restored.getCollectionId(mContext)).isEqualTo(collectionId);
        assertThat(restored.getWallpaperManagerFlag()).isEqualTo(flag);
        assertThat(restored.getImageWallpaperUri()).isEqualTo(imageWallpaperUri);
    }
}
