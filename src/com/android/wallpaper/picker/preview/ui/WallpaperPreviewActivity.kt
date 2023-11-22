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
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.android.wallpaper.R
import com.android.wallpaper.model.LiveWallpaperInfo
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.model.wallpaper.WallpaperModel
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.BasePreviewActivity
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.ActivityUtils
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.converter.DefaultWallpaperModelFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** This activity holds the flow for the preview screen. */
@AndroidEntryPoint(BasePreviewActivity::class)
class WallpaperPreviewActivity :
    Hilt_WallpaperPreviewActivity(), AppbarFragment.AppbarFragmentHost {
    private val viewModel: WallpaperPreviewViewModel by viewModels()
    @ApplicationContext @Inject lateinit var appContext: Context
    @Inject lateinit var displayUtils: DisplayUtils
    @Inject lateinit var wallpaperModelFactory: DefaultWallpaperModelFactory
    @Inject lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_wallpaper_preview)
        // Fits screen to navbar and statusbar
        WindowCompat.setDecorFitsSystemWindows(window, ActivityUtils.isSUWMode(this))
        val wallpaper =
            checkNotNull(intent.getParcelableExtra(EXTRA_WALLPAPER_INFO, WallpaperInfo::class.java))
        wallpaperPreviewRepository.setWallpaperModel(wallpaper.convertToWallpaperModel())
    }

    override fun onUpArrowPressed() {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun isUpArrowSupported(): Boolean {
        return !ActivityUtils.isSUWMode(baseContext)
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation =
            if (displayUtils.isOnWallpaperDisplay(this)) ActivityInfo.SCREEN_ORIENTATION_USER
            else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (isInMultiWindowMode) {
            Toast.makeText(this, R.string.wallpaper_exit_split_screen, Toast.LENGTH_SHORT).show()
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun WallpaperInfo.convertToWallpaperModel(): WallpaperModel {
        return if (this is LiveWallpaperInfo) {
            wallpaperModelFactory.getWallpaperModel(appContext, this)
        } else {
            wallpaperModelFactory.getWallpaperModel(appContext, this)
        }
    }

    companion object {
        /**
         * Returns a new [Intent] that can be used to start [WallpaperPreviewActivity].
         *
         * @param context application context.
         * @param wallpaperInfo selected by user for editing preview.
         * @param isNewTask true to launch at a new task.
         *
         * TODO(b/291761856): Use wallpaper model to replace wallpaper info.
         */
        fun newIntent(
            context: Context,
            wallpaperInfo: WallpaperInfo,
            isNewTask: Boolean = false,
        ): Intent {
            val intent = Intent(context.applicationContext, WallpaperPreviewActivity::class.java)
            if (isNewTask) {
                // TODO(b/291761856): When going back to main screen, use startActivity instead of
                //                    onActivityResult, which won't work.
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            intent.putExtra(EXTRA_WALLPAPER_INFO, wallpaperInfo)
            return intent
        }
    }
}
