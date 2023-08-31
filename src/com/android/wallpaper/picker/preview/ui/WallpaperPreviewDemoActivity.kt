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
package com.android.wallpaper.picker.preview.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.android.wallpaper.R
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.picker.BasePreviewActivity
import com.android.wallpaper.picker.preview.ui.binder.StaticWallpaperPreviewBinder
import com.android.wallpaper.picker.preview.ui.viewmodel.StaticWallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.RtlUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** A demo Activity showing wallpaper preview rendering built in the recommended architecture. */
@AndroidEntryPoint(BasePreviewActivity::class)
class WallpaperPreviewDemoActivity : Hilt_WallpaperPreviewDemoActivity() {

    private val viewModel: StaticWallpaperPreviewViewModel by viewModels()
    @Inject lateinit var displayUtils: DisplayUtils
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_preview_demo)
        val wallpaper = intent.getParcelableExtra(EXTRA_WALLPAPER_INFO, WallpaperInfo::class.java)
        viewModel.initializeViewModel(applicationContext, wallpaper)
        StaticWallpaperPreviewBinder.bind(
            requireViewById(R.id.wallpaper_preview),
            viewModel,
            this,
            displayUtils.isSingleDisplayOrUnfoldedHorizontalHinge(this),
            RtlUtils.isRtl(applicationContext),
        )
    }

    companion object {
        fun newIntent(
            context: Context,
            wallpaperInfo: WallpaperInfo,
        ): Intent {
            return Intent(context.applicationContext, WallpaperPreviewDemoActivity::class.java)
                .apply { putExtra(EXTRA_WALLPAPER_INFO, wallpaperInfo) }
        }
    }
}
