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
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.android.wallpaper.R
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.WallpaperPersister
import com.android.wallpaper.picker.BasePreviewActivity
import com.android.wallpaper.picker.preview.ui.binder.StaticWallpaperPreviewBinder
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.RtlUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils.setUpSurface
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/** A demo Activity showing wallpaper preview rendering built in the recommended architecture. */
@AndroidEntryPoint(BasePreviewActivity::class)
class WallpaperPreviewDemoActivity : Hilt_WallpaperPreviewDemoActivity() {

    private val viewModel: WallpaperPreviewViewModel by viewModels()
    @Inject lateinit var displayUtils: DisplayUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_preview_demo)
        val wallpaper =
            checkNotNull(intent.getParcelableExtra(EXTRA_WALLPAPER_INFO, WallpaperInfo::class.java))
        if (wallpaper is LiveWallpaperInfo) {
            val surfaceView = requireViewById<SurfaceView>(R.id.wallpaper_surface)
            surfaceView.holder.addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        lifecycleScope.launch {
                            surfaceView.setUpSurface(applicationContext)
                            WallpaperConnectionUtils.connect(
                                applicationContext,
                                lifecycleScope,
                                wallpaper.wallpaperComponent,
                                WallpaperPersister.DEST_LOCK_SCREEN,
                                surfaceView,
                            )
                        }
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {}
                }
            )
        } else {
            viewModel.initializeViewModel(
                context = applicationContext,
                wallpaper = wallpaper,
            )
            StaticWallpaperPreviewBinder.bind(
                fullResImageView = requireViewById(R.id.full_res_image),
                lowResImageView = requireViewById(R.id.low_res_image),
                viewModel = viewModel.getStaticWallpaperPreviewViewModel(),
                viewLifecycleOwner = this,
                isSingleDisplayOrUnfoldedHorizontalHinge =
                    displayUtils.isSingleDisplayOrUnfoldedHorizontalHinge(this),
                isRtl = RtlUtils.isRtl(applicationContext),
            )
        }
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
