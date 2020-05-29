/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.wallpaper.widget;

import android.app.WallpaperColors;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.asset.BitmapCachingAsset;

/** A class to load the {@link WallpaperColors} from wallpaper {@link Asset}. */
public class WallpaperColorsLoader {
    private static final String TAG = "WallpaperColorsLoader";

    /** Callback of loading {@link WallpaperColors}. */
    public interface Callback {
        /** Gets called when {@link WallpaperColors} parsing is succeed. */
        void onSuccess(WallpaperColors colors);

        /** Gets called when {@link WallpaperColors} parsing is failed. */
        default void onFailure() {
            Log.i(TAG, "Can't get wallpaper colors from a null bitmap.");
        }
    }

    /** Gets the {@link WallpaperColors} from the wallpaper {@link Asset}. */
    public static void getWallpaperColors(Context context, @NonNull Asset asset,
                                          int targetWidth, int targetHeight,
                                          @NonNull Callback callback) {
        new BitmapCachingAsset(context, asset).decodeBitmap(targetWidth, targetHeight, bitmap -> {
            if (bitmap != null) {
                boolean shouldRecycle = false;
                if (bitmap.getConfig() == Bitmap.Config.HARDWARE) {
                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                    shouldRecycle = true;
                }
                callback.onSuccess(WallpaperColors.fromBitmap(bitmap));
                if (shouldRecycle) {
                    bitmap.recycle();
                }
            } else {
                callback.onFailure();
            }
        });
    }
}
