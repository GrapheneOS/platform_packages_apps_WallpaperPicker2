/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.wallpaper.testing

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.model.CurrentWallpaperInfo
import java.util.concurrent.atomic.AtomicReference

/**
 * Minimal fake of [CurrentWallpaperInfo] for testing, initially created for use in
 * DefaultGoogleWallpaperPreferencesTest.
 */
class FakeCurrentWallpaperInfo(
    attributions: List<String?>?,
    actionUrl: String?,
    collectionId: String?,
    @WallpaperManager.SetWallpaperFlags wallpaperManagerFlag: Int,
    private val pixelColor: Int,
    private val id: String,
    imageWallpaperUri: Uri? = null,
) :
    CurrentWallpaperInfo(
        attributions,
        actionUrl,
        collectionId,
        wallpaperManagerFlag,
        imageWallpaperUri,
    ) {
    private var asset = AtomicReference<TestAsset>()

    constructor(
        pixelColor: Int,
        id: String,
    ) : this(emptyList(), "", "", WallpaperManager.FLAG_SYSTEM, pixelColor, id)

    override fun getAsset(context: Context): Asset {
        return asset.updateAndGet { it ?: TestAsset(pixelColor, false) }
    }

    override fun getWallpaperId(): String {
        return id
    }
}
