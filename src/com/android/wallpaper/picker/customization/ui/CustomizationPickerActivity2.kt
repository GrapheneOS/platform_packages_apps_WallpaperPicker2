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

package com.android.wallpaper.picker.customization.ui

import android.annotation.TargetApi
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.wallpaper.R
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel
import com.android.wallpaper.picker.common.preview.data.repository.PersistentWallpaperModelRepository
import com.android.wallpaper.picker.common.preview.ui.binder.WorkspaceCallbackBinder
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.ToolbarBinder
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.util.ActivityUtils
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.converter.WallpaperModelFactory
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

@AndroidEntryPoint(AppCompatActivity::class)
class CustomizationPickerActivity2 :
    Hilt_CustomizationPickerActivity2(), AppbarFragment.AppbarFragmentHost {

    interface ActivityEnterAnimationCallback {
        /**
         * The callback is called when Fragment's parent Activity is the first time created and the
         * enter animation is completed.
         */
        fun onEnterAnimationCompleteAfterActivityCreated()
    }

    @Inject lateinit var multiPanesChecker: MultiPanesChecker
    @Inject lateinit var customizationOptionUtil: CustomizationOptionUtil
    @Inject lateinit var customizationOptionsBinder: CustomizationOptionsBinder
    @Inject lateinit var workspaceCallbackBinder: WorkspaceCallbackBinder
    @Inject lateinit var toolbarBinder: ToolbarBinder
    @Inject lateinit var wallpaperModelFactory: WallpaperModelFactory
    @Inject lateinit var persistentWallpaperModelRepository: PersistentWallpaperModelRepository
    @Inject lateinit var displayUtils: DisplayUtils
    @Inject @BackgroundDispatcher lateinit var backgroundScope: CoroutineScope
    @Inject @MainDispatcher lateinit var mainScope: CoroutineScope
    @Inject lateinit var wallpaperConnectionUtils: WallpaperConnectionUtils
    @Inject lateinit var colorUpdateViewModel: ColorUpdateViewModel
    @Inject lateinit var clockViewFactory: ClockViewFactory

    private var configuration: Configuration? = null
    private val categoriesViewModel: CategoriesViewModel by viewModels()
    private var isInitialCreation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            // Activity is being restored, not initial creation
            isInitialCreation = false
        }

        if (
            multiPanesChecker.isMultiPanesEnabled(this) &&
                !ActivityUtils.isLaunchedFromSettingsTrampoline(intent) &&
                !ActivityUtils.isLaunchedFromSettingsRelated(intent)
        ) {
            // If the device supports multi panes, we check if the activity is launched by settings.
            // If not, we need to start an intent to have settings launch the customization
            // activity. In case it is a two-pane situation and the activity should be embedded in
            // the settings app, instead of in the full screen.
            val multiPanesIntent = multiPanesChecker.getMultiPanesIntent(intent)
            ActivityUtils.startActivityForResultSafely(
                this, /* activity */
                multiPanesIntent,
                0, /* requestCode */
            )
            finish()
            return
        }
        categoriesViewModel.initialize()
        configuration = Configuration(resources.configuration)
        colorUpdateViewModel.updateDarkModeAndColors()
        colorUpdateViewModel.setPreviewEnabled(!displayUtils.isLargeScreenOrUnfoldedDisplay(this))

        setContentView(R.layout.activity_cusomization_picker2)
        WindowCompat.setDecorFitsSystemWindows(window, ActivityUtils.isSUWMode(this))

        ColorUpdateBinder.bind(
            setColor = { color ->
                requireViewById<FrameLayout>(R.id.fragment_container).setBackgroundColor(color)
            },
            color = colorUpdateViewModel.colorSurfaceContainer,
            shouldAnimate = {
                supportFragmentManager.findFragmentById(R.id.fragment_container) is
                    CustomizationPickerFragment2
            },
            lifecycleOwner = this,
        )

        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, CustomizationPickerFragment2())
                .commit()
        }
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        if (isInitialCreation) {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container)
                    as? CustomizationPickerFragment2
            fragment?.onEnterAnimationCompleteAfterActivityCreated()
            isInitialCreation = false
        }
    }

    override fun onUpArrowPressed() {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun isUpArrowSupported(): Boolean {
        return !ActivityUtils.isSUWMode(baseContext)
    }

    @TargetApi(36)
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configuration?.let {
            val diff = newConfig.diff(it)
            val isAssetsPathsChange = diff and ActivityInfo.CONFIG_ASSETS_PATHS != 0
            val isUiModeChange = diff and ActivityInfo.CONFIG_UI_MODE != 0
            if (isAssetsPathsChange) {
                colorUpdateViewModel.updateColors()
            }
            if (isUiModeChange) {
                colorUpdateViewModel.updateDarkModeAndColors()
            }
        }
        configuration?.setTo(newConfig)
    }
}
