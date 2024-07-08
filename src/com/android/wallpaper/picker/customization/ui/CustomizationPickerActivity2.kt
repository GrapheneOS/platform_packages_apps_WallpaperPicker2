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

import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toolbar
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionLayout.TransitionListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.WindowCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.app.tracing.coroutines.launch
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.picker.common.preview.ui.binder.BasePreviewBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationPickerBinder2
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.view.adapter.PreviewPagerAdapter
import com.android.wallpaper.picker.customization.ui.view.transformer.PreviewPagerPageTransformer
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.util.ActivityUtils
import com.android.wallpaper.util.WallpaperConnection
import com.android.wallpaper.util.converter.WallpaperModelFactory
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint(AppCompatActivity::class)
class CustomizationPickerActivity2 : Hilt_CustomizationPickerActivity2() {

    @Inject lateinit var multiPanesChecker: MultiPanesChecker
    @Inject lateinit var customizationOptionUtil: CustomizationOptionUtil
    @Inject lateinit var customizationOptionsBinder: CustomizationOptionsBinder
    @Inject lateinit var wallpaperModelFactory: WallpaperModelFactory
    @Inject lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository
    @Inject @BackgroundDispatcher lateinit var backgroundScope: CoroutineScope

    private var fullyCollapsed = false

    private val customizationPickerViewModel: CustomizationPickerViewModel2 by viewModels()
    private var customizationOptionFloatingSheetMap: Map<CustomizationOption, View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        }

        setContentView(R.layout.activity_cusomization_picker2)
        WindowCompat.setDecorFitsSystemWindows(window, ActivityUtils.isSUWMode(this))

        setupToolbar(requireViewById(R.id.toolbar_container))

        val rootView = requireViewById<MotionLayout>(R.id.picker_motion_layout)

        customizationOptionFloatingSheetMap =
            customizationOptionUtil.initFloatingSheet(
                rootView.requireViewById<FrameLayout>(
                    R.id.customization_option_floating_sheet_container
                ),
                layoutInflater,
            )
        rootView.setTransitionListener(
            object : EmptyTransitionListener {
                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    if (
                        currentId == R.id.expanded_header_primary ||
                            currentId == R.id.collapsed_header_primary
                    ) {
                        rootView.setTransition(R.id.transition_primary)
                    }
                }
            }
        )

        val previewViewModel = customizationPickerViewModel.basePreviewViewModel
        previewViewModel.setWhichPreview(WallpaperConnection.WhichPreview.EDIT_CURRENT)
        previewViewModel.setIsWallpaperColorPreviewEnabled(
            !InjectorProvider.getInjector().isCurrentSelectedColorPreset(applicationContext)
        )

        // WIP, the current preview flow only allows setting one wallpaper model to preview.
        // To test the preview flow we get and set the current home wallpaper model.
        // TODO (b/348462236): add ability preview current wallpaper models, lock and home
        val wallpaperInfoFactory =
            InjectorProvider.getInjector().getCurrentWallpaperInfoFactory(applicationContext)
        backgroundScope.launch {
            wallpaperInfoFactory.createCurrentWallpaperInfos(
                applicationContext,
                /* forceRefresh= */ true,
            ) { homeWallpaper, _, _ ->
                val homeWallpaperModel =
                    wallpaperModelFactory.getWallpaperModel(applicationContext, homeWallpaper)
                wallpaperPreviewRepository.setWallpaperModel(homeWallpaperModel)
            }
        }

        initPreviewPager()

        val optionContainer = requireViewById<MotionLayout>(R.id.customization_option_container)
        // The collapsed header height should be updated when option container's height is known
        optionContainer.doOnPreDraw {
            // The bottom navigation bar height
            val navBarHeight =
                resources.getIdentifier("navigation_bar_height", "dimen", "android").let {
                    if (it > 0) {
                        resources.getDimensionPixelSize(it)
                    } else 0
                }
            val collapsedHeaderHeight = rootView.height - optionContainer.height - navBarHeight
            if (
                collapsedHeaderHeight >
                    resources.getDimensionPixelSize(
                        R.dimen.customization_picker_preview_header_collapsed_height
                    )
            ) {
                rootView
                    .getConstraintSet(R.id.collapsed_header_primary)
                    ?.constrainHeight(R.id.preview_header, collapsedHeaderHeight)
                rootView.setTransition(R.id.transition_primary)
            }
        }

        val onBackPressed =
            CustomizationPickerBinder2.bind(
                view = rootView,
                lockScreenCustomizationOptionEntries = initCustomizationOptionEntries(LOCK_SCREEN),
                homeScreenCustomizationOptionEntries = initCustomizationOptionEntries(HOME_SCREEN),
                viewModel = customizationPickerViewModel,
                customizationOptionsBinder = customizationOptionsBinder,
                lifecycleOwner = this,
                navigateToPrimary = {
                    if (rootView.currentState == R.id.secondary) {
                        rootView.transitionToState(
                            if (fullyCollapsed) R.id.collapsed_header_primary
                            else R.id.expanded_header_primary
                        )
                    }
                },
                navigateToSecondary = { screen ->
                    if (rootView.currentState != R.id.secondary) {
                        setCustomizationOptionFloatingSheet(rootView, screen) {
                            fullyCollapsed = rootView.progress == 1.0f
                            rootView.transitionToState(R.id.secondary)
                        }
                    }
                },
            )

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val isOnBackPressedHandled = onBackPressed()
                    if (!isOnBackPressedHandled) {
                        remove()
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun setupToolbar(toolbarContainer: AppBarLayout) {
        toolbarContainer.setBackgroundColor(Color.TRANSPARENT)
        val toolbar = toolbarContainer.requireViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.app_name)
        toolbar.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun initCustomizationOptionEntries(
        screen: Screen,
    ): List<Pair<CustomizationOption, View>> {
        val optionEntriesContainer =
            requireViewById<LinearLayout>(
                when (screen) {
                    LOCK_SCREEN -> R.id.lock_customization_option_container
                    HOME_SCREEN -> R.id.home_customization_option_container
                }
            )
        val optionEntries =
            customizationOptionUtil.getOptionEntries(screen, optionEntriesContainer, layoutInflater)
        optionEntries.onEachIndexed { index, (option, view) ->
            val isFirst = index == 0
            val isLast = index == optionEntries.size - 1
            view.setBackgroundResource(
                if (isFirst) R.drawable.customization_option_entry_top_background
                else if (isLast) R.drawable.customization_option_entry_bottom_background
                else R.drawable.customization_option_entry_background
            )
            optionEntriesContainer.addView(view)
        }
        return optionEntries
    }

    private fun initPreviewPager() {
        val pager = requireViewById<ViewPager2>(R.id.preview_pager)
        val previewViewModel = customizationPickerViewModel.basePreviewViewModel
        pager.apply {
            adapter = PreviewPagerAdapter { viewHolder, position ->
                val previewCard = viewHolder.itemView.requireViewById<View>(R.id.preview_card)

                BasePreviewBinder.bind(
                    applicationContext,
                    previewCard,
                    previewViewModel,
                    if (position == 0) {
                        LOCK_SCREEN
                    } else {
                        HOME_SCREEN
                    },
                    DeviceDisplayType.SINGLE,
                    previewViewModel.wallpaperDisplaySize.value,
                    this@CustomizationPickerActivity2,
                    true
                )
            }
            // Disable over scroll
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            // The neighboring view should be inflated when pager is rendered
            offscreenPageLimit = 1
            // When pager's height changes, request transform to recalculate the preview offset
            // to make sure correct space between the previews.
            // TODO (b/348462236): figure out how to scale surface view content with layout change
            addOnLayoutChangeListener { view, _, _, _, _, _, topWas, _, bottomWas ->
                val isHeightChanged = (bottomWas - topWas) != view.height
                if (isHeightChanged) {
                    pager.requestTransform()
                }
            }
        }

        // Only when pager is laid out, we can get the width and set the preview's offset correctly
        pager.doOnLayout {
            (it as ViewPager2).apply {
                setPageTransformer(PreviewPagerPageTransformer(Point(width, height)))
            }
        }
    }

    /**
     * Set customization option floating sheet to the floating sheet container and get the new
     * container's height for repositioning the preview's guideline.
     */
    private fun setCustomizationOptionFloatingSheet(
        motionContainer: MotionLayout,
        option: CustomizationOption,
        onComplete: () -> Unit
    ) {
        val view = customizationOptionFloatingSheetMap?.get(option) ?: return

        val floatingSheetContainer =
            requireViewById<FrameLayout>(R.id.customization_option_floating_sheet_container)
        val guideline = requireViewById<Guideline>(R.id.preview_guideline_in_secondary_screen)
        floatingSheetContainer.removeAllViews()
        floatingSheetContainer.addView(view)

        view.doOnPreDraw {
            val height = view.height
            guideline.setGuidelineEnd(height)
            floatingSheetContainer.translationY = 0.0f
            floatingSheetContainer.alpha = 0.0f
            // Update the motion container
            motionContainer.getConstraintSet(R.id.expanded_header_primary)?.apply {
                setTranslationY(R.id.customization_option_floating_sheet_container, 0.0f)
                setAlpha(R.id.customization_option_floating_sheet_container, 0.0f)
                constrainHeight(
                    R.id.customization_option_floating_sheet_container,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }
            motionContainer.getConstraintSet(R.id.collapsed_header_primary)?.apply {
                setTranslationY(R.id.customization_option_floating_sheet_container, 0.0f)
                setAlpha(R.id.customization_option_floating_sheet_container, 0.0f)
                constrainHeight(
                    R.id.customization_option_floating_sheet_container,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }
            motionContainer.getConstraintSet(R.id.secondary)?.apply {
                setGuidelineEnd(R.id.preview_guideline_in_secondary_screen, height)
                setTranslationY(
                    R.id.customization_option_floating_sheet_container,
                    -height.toFloat()
                )
                setAlpha(R.id.customization_option_floating_sheet_container, 1.0f)
                constrainHeight(
                    R.id.customization_option_floating_sheet_container,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }
            onComplete()
        }
    }

    interface EmptyTransitionListener : TransitionListener {
        override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {
            // Do nothing intended
        }

        override fun onTransitionChange(
            motionLayout: MotionLayout?,
            startId: Int,
            endId: Int,
            progress: Float
        ) {
            // Do nothing intended
        }

        override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
            // Do nothing intended
        }

        override fun onTransitionTrigger(
            motionLayout: MotionLayout?,
            triggerId: Int,
            positive: Boolean,
            progress: Float
        ) {
            // Do nothing intended
        }
    }
}
