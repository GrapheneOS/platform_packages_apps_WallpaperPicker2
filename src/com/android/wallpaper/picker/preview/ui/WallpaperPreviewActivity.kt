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

import android.app.WindowConfiguration.WINDOWING_MODE_FREEFORM
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.ImageWallpaperInfo
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.BasePreviewActivity
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel
import com.android.wallpaper.picker.common.preview.data.repository.PersistentWallpaperModelRepository
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.picker.preview.data.repository.CreativeEffectsRepository
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.data.util.LiveWallpaperDownloader
import com.android.wallpaper.picker.preview.ui.fragment.SmallPreviewFragment
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewActionsViewModel.Companion.getEditActivityIntent
import com.android.wallpaper.picker.preview.ui.viewmodel.PreviewActionsViewModel.Companion.isNewCreativeWallpaper
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.ActivityUtils
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.WallpaperConnection
import com.android.wallpaper.util.converter.WallpaperModelFactory
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** This activity holds the flow for the preview screen. */
@AndroidEntryPoint(BasePreviewActivity::class)
class WallpaperPreviewActivity :
    Hilt_WallpaperPreviewActivity(), AppbarFragment.AppbarFragmentHost {
    @ApplicationContext @Inject lateinit var appContext: Context
    @Inject lateinit var displayUtils: DisplayUtils
    @Inject lateinit var wallpaperModelFactory: WallpaperModelFactory
    @Inject lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository
    @Inject lateinit var imageEffectsRepository: ImageEffectsRepository
    @Inject lateinit var creativeEffectsRepository: CreativeEffectsRepository
    @Inject lateinit var persistentWallpaperModelRepository: PersistentWallpaperModelRepository
    @Inject lateinit var liveWallpaperDownloader: LiveWallpaperDownloader
    @MainDispatcher @Inject lateinit var mainScope: CoroutineScope
    @Inject lateinit var wallpaperConnectionUtils: WallpaperConnectionUtils

    private var refreshCreativeCategories: Boolean? = null

    private val wallpaperPreviewViewModel: WallpaperPreviewViewModel by viewModels()
    private val categoriesViewModel: CategoriesViewModel by viewModels()

    private val isNewPickerUi = BaseFlags.get().isNewPickerUi()
    private val isCategoriesRefactorEnabled =
        BaseFlags.get().isWallpaperCategoryRefactoringEnabled()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        super.onCreate(savedInstanceState)
        enforcePortraitForHandheldAndFoldedDisplay()
        wallpaperPreviewViewModel.updateDisplayConfiguration()
        wallpaperPreviewViewModel.setIsWallpaperColorPreviewEnabled(
            !InjectorProvider.getInjector().isCurrentSelectedColorPreset(appContext)
        )
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_wallpaper_preview)

        if (isCategoriesRefactorEnabled) {
            refreshCreativeCategories = intent.getBooleanExtra(SHOULD_CATEGORY_REFRESH, false)
        }

        val wallpaper: WallpaperModel? =
            if (isNewPickerUi || isCategoriesRefactorEnabled) {
                persistentWallpaperModelRepository.wallpaperModel.value
                    ?: intent
                        .getParcelableExtra(EXTRA_WALLPAPER_INFO, WallpaperInfo::class.java)
                        ?.convertToWallpaperModel()
            } else {
                intent
                    .getParcelableExtra(EXTRA_WALLPAPER_INFO, WallpaperInfo::class.java)
                    ?.convertToWallpaperModel()
            }

        wallpaper ?: throw UnsupportedOperationException()

        val navController =
            (supportFragmentManager.findFragmentById(R.id.wallpaper_preview_nav_host)
                    as NavHostFragment)
                .navController
        val graph = navController.navInflater.inflate(R.navigation.wallpaper_preview_nav_graph)
        val startDestinationArgs: Bundle? =
            (wallpaper as? WallpaperModel.LiveWallpaperModel)
                ?.let {
                    if (it.isNewCreativeWallpaper()) it.getNewCreativeWallpaperArgs() else null
                }
                ?.also {
                    // For creating a new creative wallpaper, replace the default start destination
                    // with CreativeEditPreviewFragment.
                    graph.setStartDestination(R.id.creativeEditPreviewFragment)
                }
        navController.setGraph(graph, startDestinationArgs)
        // Fits screen to navbar and statusbar
        WindowCompat.setDecorFitsSystemWindows(window, ActivityUtils.isSUWMode(this))
        val isAssetIdPresent = intent.getBooleanExtra(IS_ASSET_ID_PRESENT, false)
        wallpaperPreviewViewModel.isNewTask = intent.getBooleanExtra(IS_NEW_TASK, false)
        if (savedInstanceState == null) {
            wallpaperPreviewRepository.setWallpaperModel(wallpaper)
        }
        val whichPreview =
            if (isAssetIdPresent) WallpaperConnection.WhichPreview.EDIT_NON_CURRENT
            else WallpaperConnection.WhichPreview.EDIT_CURRENT
        wallpaperPreviewViewModel.setWhichPreview(whichPreview)
        if (wallpaper is WallpaperModel.StaticWallpaperModel) {
            wallpaper.staticWallpaperData.cropHints?.let {
                wallpaperPreviewViewModel.setCropHints(it)
            }
        }
        if (
            (wallpaper as? WallpaperModel.StaticWallpaperModel)?.downloadableWallpaperData != null
        ) {
            liveWallpaperDownloader.initiateDownloadableService(
                this,
                wallpaper,
                registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
            )
        }

        val creativeWallpaperEffectData =
            (wallpaper as? WallpaperModel.LiveWallpaperModel)
                ?.creativeWallpaperData
                ?.creativeWallpaperEffectsData
        if (creativeWallpaperEffectData != null) {
            lifecycleScope.launch {
                creativeEffectsRepository.initializeEffect(creativeWallpaperEffectData)
            }
        } else if (
            (wallpaper as? WallpaperModel.StaticWallpaperModel)?.imageWallpaperData != null &&
                imageEffectsRepository.areEffectsAvailable()
        ) {
            lifecycleScope.launch {
                imageEffectsRepository.initializeEffect(
                    staticWallpaperModel = wallpaper,
                    onWallpaperModelUpdated = { wallpaper ->
                        wallpaperPreviewRepository.setWallpaperModel(wallpaper)
                    },
                )
            }
        }
    }

    override fun onUpArrowPressed() {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun isUpArrowSupported(): Boolean {
        return !ActivityUtils.isSUWMode(baseContext)
    }

    override fun onResume() {
        super.onResume()
        val isWindowingModeFreeform =
            resources.configuration.windowConfiguration.windowingMode == WINDOWING_MODE_FREEFORM
        if (isInMultiWindowMode && !isWindowingModeFreeform) {
            Toast.makeText(this, R.string.wallpaper_exit_split_screen, Toast.LENGTH_SHORT).show()
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            persistentWallpaperModelRepository.cleanup()
            // ImageEffectsRepositoryImpl is Activity-Retained Scoped, and its injected
            // EffectsController is Singleton scoped. Therefore, persist state on config change
            // restart, and only destroy when activity is finishing.
            imageEffectsRepository.destroy()
        }
        creativeEffectsRepository.destroy()
        liveWallpaperDownloader.cleanup()
        // TODO(b/333879532): Only disconnect when leaving the Activity without introducing black
        //  preview. If onDestroy is caused by an orientation change, we should keep the connection
        //  to avoid initiating the engines again.
        // TODO(b/328302105): MainScope ensures the job gets done non-blocking even if the
        //   activity has been destroyed already. Consider making this part of
        //   WallpaperConnectionUtils.
        mainScope.launch { wallpaperConnectionUtils.disconnectAll(appContext) }

        refreshCreativeCategories?.let {
            if (it) {
                categoriesViewModel.refreshCategory()
            }
        }

        super.onDestroy()
    }

    private fun WallpaperInfo.convertToWallpaperModel(): WallpaperModel {
        return wallpaperModelFactory.getWallpaperModel(appContext, this)
    }

    companion object {
        /**
         * Returns a new [Intent] for the new picker UI that can be used to start
         * [WallpaperPreviewActivity].
         *
         * @param context application context.
         * @param isNewTask true to launch at a new task.
         */
        fun newIntent(
            context: Context,
            isAssetIdPresent: Boolean,
            isViewAsHome: Boolean = false,
            isNewTask: Boolean = false,
        ): Intent {
            val isNewPickerUi = BaseFlags.get().isNewPickerUi()
            val isCategoriesRefactorEnabled =
                BaseFlags.get().isWallpaperCategoryRefactoringEnabled()
            if (!(isNewPickerUi || isCategoriesRefactorEnabled))
                throw UnsupportedOperationException()
            val intent = Intent(context.applicationContext, WallpaperPreviewActivity::class.java)
            if (isNewTask) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            intent.putExtra(IS_ASSET_ID_PRESENT, isAssetIdPresent)
            intent.putExtra(EXTRA_VIEW_AS_HOME, isViewAsHome)
            intent.putExtra(IS_NEW_TASK, isNewTask)
            return intent
        }

        /**
         * Returns a new [Intent] for the new picker UI that can be used to start
         * [WallpaperPreviewActivity].
         *
         * @param context application context.
         * @param isNewTask true to launch at a new task.
         * @param shouldCategoryRefresh specified the category type
         */
        fun newIntent(
            context: Context,
            isAssetIdPresent: Boolean,
            isViewAsHome: Boolean = false,
            isNewTask: Boolean = false,
            shouldCategoryRefresh: Boolean
        ): Intent {
            val isNewPickerUi = BaseFlags.get().isNewPickerUi()
            val isCategoriesRefactorEnabled =
                BaseFlags.get().isWallpaperCategoryRefactoringEnabled()
            if (!(isNewPickerUi || isCategoriesRefactorEnabled))
                throw UnsupportedOperationException()
            val intent = Intent(context.applicationContext, WallpaperPreviewActivity::class.java)
            if (isNewTask) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            intent.putExtra(IS_ASSET_ID_PRESENT, isAssetIdPresent)
            intent.putExtra(EXTRA_VIEW_AS_HOME, isViewAsHome)
            intent.putExtra(IS_NEW_TASK, isNewTask)
            intent.putExtra(SHOULD_CATEGORY_REFRESH, shouldCategoryRefresh)
            return intent
        }

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
            isAssetIdPresent: Boolean,
            isViewAsHome: Boolean = false,
            isNewTask: Boolean = false,
        ): Intent {
            val intent = Intent(context.applicationContext, WallpaperPreviewActivity::class.java)
            if (isNewTask) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            intent.putExtra(EXTRA_WALLPAPER_INFO, wallpaperInfo)
            intent.putExtra(IS_ASSET_ID_PRESENT, isAssetIdPresent)
            intent.putExtra(EXTRA_VIEW_AS_HOME, isViewAsHome)
            intent.putExtra(IS_NEW_TASK, isNewTask)
            return intent
        }

        /**
         * Returns a new [Intent] that can be used to start [WallpaperPreviewActivity].
         *
         * @param context application context.
         * @param wallpaperInfo selected by user for editing preview.
         * @param isNewTask true to launch at a new task.
         * @param shouldRefreshCategory specifies the type of category this wallpaper belongs
         *
         * TODO(b/291761856): Use wallpaper model to replace wallpaper info.
         */
        fun newIntent(
            context: Context,
            wallpaperInfo: WallpaperInfo,
            isAssetIdPresent: Boolean,
            isViewAsHome: Boolean = false,
            isNewTask: Boolean = false,
            shouldRefreshCategory: Boolean
        ): Intent {
            val intent = Intent(context.applicationContext, WallpaperPreviewActivity::class.java)
            if (isNewTask) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            intent.putExtra(EXTRA_WALLPAPER_INFO, wallpaperInfo)
            intent.putExtra(IS_ASSET_ID_PRESENT, isAssetIdPresent)
            intent.putExtra(EXTRA_VIEW_AS_HOME, isViewAsHome)
            intent.putExtra(IS_NEW_TASK, isNewTask)
            intent.putExtra(SHOULD_CATEGORY_REFRESH, shouldRefreshCategory)
            return intent
        }

        /**
         * Returns a new [Intent] that can be used to start [WallpaperPreviewActivity], explicitly
         * propagating any permissions on the wallpaper data to the new [Intent].
         *
         * @param context application context.
         * @param wallpaperInfo selected by user for editing preview.
         * @param isNewTask true to launch at a new task.
         *
         * TODO(b/291761856): Use wallpaper model to replace wallpaper info.
         */
        fun newIntent(
            context: Context,
            originalIntent: Intent,
            isAssetIdPresent: Boolean,
            isViewAsHome: Boolean = false,
            isNewTask: Boolean = false,
        ): Intent {
            val data = originalIntent.data
            val intent =
                newIntent(
                    context,
                    ImageWallpaperInfo(data),
                    isAssetIdPresent,
                    isViewAsHome,
                    isNewTask
                )
            // Both these lines are required for permission propagation
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setData(data)
            return intent
        }

        private fun WallpaperModel.LiveWallpaperModel.getNewCreativeWallpaperArgs() =
            Bundle().apply {
                putParcelable(
                    SmallPreviewFragment.ARG_EDIT_INTENT,
                    liveWallpaperData.getEditActivityIntent(true),
                )
            }
    }

    /**
     * If the display is a handheld display or a folded display from a foldable, we enforce the
     * activity to be portrait.
     *
     * This method should be called upon initialization of this activity, and whenever there is a
     * configuration change.
     */
    private fun enforcePortraitForHandheldAndFoldedDisplay() {
        val wantedOrientation =
            if (displayUtils.isLargeScreenOrUnfoldedDisplay(this))
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (requestedOrientation != wantedOrientation) {
            requestedOrientation = wantedOrientation
        }
    }
}
