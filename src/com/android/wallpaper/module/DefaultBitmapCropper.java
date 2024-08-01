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

import android.graphics.Rect;

import com.android.wallpaper.asset.Asset;

/**
 * Default implementation of BitmapCropper, which actually crops and scales bitmaps.
 */
public class DefaultBitmapCropper implements BitmapCropper {

    @Override
    public void cropAndScaleBitmap(Asset asset, float scale, Rect cropRect,
            boolean isRtl, Callback callback) {
        int targetWidth = (int) (cropRect.width() / scale);
        int targetHeight = (int) (cropRect.height() / scale);
        // Giving the target width and height can down-sample a large bitmap to a smaller target
        // size, which saves memory use.
        asset.decodeBitmapRegion(cropRect, targetWidth, targetHeight, isRtl,
                bitmap -> {
                    if (bitmap == null) {
                        callback.onError(null);
                        return;
                    }
                    callback.onBitmapCropped(bitmap);
                });
    }
}
