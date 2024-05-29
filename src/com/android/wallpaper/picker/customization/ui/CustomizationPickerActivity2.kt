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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionLayout.TransitionListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.wallpaper.R
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.picker.customization.ui.binder.CustomizationPickerBinder2
import com.android.wallpaper.picker.customization.ui.view.adapter.PreviewPagerAdapter
import com.android.wallpaper.picker.customization.ui.view.transformer.PreviewPagerPageTransformer
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen
import com.android.wallpaper.util.ActivityUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(AppCompatActivity::class)
class CustomizationPickerActivity2 : Hilt_CustomizationPickerActivity2() {

    @Inject lateinit var multiPanesChecker: MultiPanesChecker

    private lateinit var viewMap: Map<PickerScreen, View>

    private var fullyCollapsed = false

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

        val motionContainer = requireViewById<MotionLayout>(R.id.picker_motion_layout)
        initBottomSheetContent(motionContainer)
        motionContainer.setTransitionListener(
            object : EmptyTransitionListener {
                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    if (
                        currentId == R.id.expanded_header_primary ||
                            currentId == R.id.collapsed_header_primary
                    ) {
                        motionContainer.setTransition(R.id.transition_primary)
                    }
                }
            }
        )

        initPreviewPager()
        val onSetScreenStateMain =
            CustomizationPickerBinder2.bind(
                view = motionContainer,
                viewModel = CustomizationPickerViewModel2(),
                lifecycleOwner = this,
                navigateToPrimary = {
                    if (motionContainer.currentState == R.id.secondary) {
                        motionContainer.transitionToState(
                            if (fullyCollapsed) R.id.collapsed_header_primary
                            else R.id.expanded_header_primary
                        )
                    }
                },
                navigateToSecondary = { screen ->
                    if (motionContainer.currentState != R.id.secondary) {
                        addCustomizePickerBottomSheet(motionContainer, screen) {
                            fullyCollapsed = motionContainer.progress == 1.0f
                            motionContainer.transitionToState(R.id.secondary)
                        }
                    }
                },
            )

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val isOnBackPressedHandled = onSetScreenStateMain()
                    if (!isOnBackPressedHandled) {
                        remove()
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun initPreviewPager() {
        val pager = requireViewById<ViewPager2>(R.id.preview_pager)
        pager.apply {
            adapter = PreviewPagerAdapter { viewHolder, position ->
                viewHolder.itemView
                    .requireViewById<View>(R.id.preview_card)
                    .setBackgroundColor(if (position == 0) Color.BLUE else Color.CYAN)
            }
            // Disable over scroll
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            // The neighboring view should be inflated when pager is rendered
            offscreenPageLimit = 1
            // When pager's height changes, request transform to recalculate the preview offset
            // to make sure correct space between the previews.
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

    private fun initBottomSheetContent(motionContainer: MotionLayout) {
        val customizationBottomSheet =
            requireViewById<FrameLayout>(R.id.customization_picker_bottom_sheet)
        // TODO(b/343300705): Move the view map to a separate class
        viewMap =
            mapOf(
                PickerScreen.CLOCK to
                    createCustomizationPickerBottomSheetView(PickerScreen.CLOCK, motionContainer)
                        .also { customizationBottomSheet.addView(it) },
                PickerScreen.SHORTCUT to
                    createCustomizationPickerBottomSheetView(PickerScreen.SHORTCUT, motionContainer)
                        .also { customizationBottomSheet.addView(it) }
            )
    }

    private fun addCustomizePickerBottomSheet(
        motionContainer: MotionLayout,
        screen: PickerScreen,
        onComplete: () -> Unit
    ) {
        val view =
            viewMap[screen]
                ?: throw IllegalStateException(
                    "$screen does not have a correspondent bottom sheet view."
                )

        val customizationBottomSheet =
            requireViewById<FrameLayout>(R.id.customization_picker_bottom_sheet)
        val guideline = requireViewById<Guideline>(R.id.preview_guideline_in_secondary_screen)
        customizationBottomSheet.removeAllViews()
        customizationBottomSheet.addView(view)

        view.doOnPreDraw {
            val height = view.height
            guideline.setGuidelineEnd(height)
            customizationBottomSheet.translationY = 0.0f
            customizationBottomSheet.alpha = 0.0f
            // Update the motion container
            motionContainer.getConstraintSet(R.id.expanded_header_primary)?.apply {
                setTranslationY(R.id.customization_picker_bottom_sheet, 0.0f)
                setAlpha(R.id.customization_picker_bottom_sheet, 0.0f)
                constrainHeight(
                    R.id.customization_picker_bottom_sheet,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }
            motionContainer.getConstraintSet(R.id.collapsed_header_primary)?.apply {
                setTranslationY(R.id.customization_picker_bottom_sheet, 0.0f)
                setAlpha(R.id.customization_picker_bottom_sheet, 0.0f)
                constrainHeight(
                    R.id.customization_picker_bottom_sheet,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }
            motionContainer.getConstraintSet(R.id.secondary)?.apply {
                setGuidelineEnd(R.id.preview_guideline_in_secondary_screen, height)
                setTranslationY(R.id.customization_picker_bottom_sheet, -height.toFloat())
                setAlpha(R.id.customization_picker_bottom_sheet, 1.0f)
                constrainHeight(
                    R.id.customization_picker_bottom_sheet,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }
            onComplete()
        }
    }

    private fun createCustomizationPickerBottomSheetView(
        screen: PickerScreen,
        motionContainer: MotionLayout
    ): View =
        when (screen) {
            PickerScreen.MAIN ->
                throw IllegalStateException(
                    "There is no bottom sheet for the primary customization picker screen."
                )
            PickerScreen.CLOCK -> R.layout.bottom_sheet_clock
            PickerScreen.SHORTCUT -> R.layout.bottom_sheet_shortcut
        }.let { layoutInflater.inflate(it, motionContainer, false) }

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
