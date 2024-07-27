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

package com.android.wallpaper.picker.customization.ui.binder

import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.picker.customization.ui.CustomizationPickerActivity2
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen.CUSTOMIZATION_OPTION
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen.MAIN
import kotlinx.coroutines.launch

object CustomizationPickerBinder2 {

    private const val ALPHA_SELECTED_PREVIEW = 1f
    private const val ALPHA_NON_SELECTED_PREVIEW = 0.4f
    private const val LOCK_SCREEN_PREVIEW_POSITION = 0
    private const val HOME_SCREEN_PREVIEW_POSITION = 1

    /**
     * @return Callback for the [CustomizationPickerActivity2] to set
     *   [CustomizationPickerViewModel2]'s screen state to null, which infers to the main screen. We
     *   need this callback to handle the back navigation in [CustomizationPickerActivity2].
     */
    fun bind(
        view: View,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>?,
        viewModel: CustomizationPickerViewModel2,
        customizationOptionsBinder: CustomizationOptionsBinder,
        lifecycleOwner: LifecycleOwner,
        navigateToPrimary: () -> Unit,
        navigateToSecondary: (screen: CustomizationOption) -> Unit,
    ): () -> Boolean {
        val optionContainer =
            view.requireViewById<MotionLayout>(R.id.customization_option_container)
        val pager = view.requireViewById<ViewPager2>(R.id.preview_pager)
        pager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    viewModel.selectPreviewScreen(
                        if (position == LOCK_SCREEN_PREVIEW_POSITION) LOCK_SCREEN else HOME_SCREEN
                    )
                }
            }
        )
        val mediumAnimTimeMs =
            view.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        pager.doOnLayout {
            // RecyclerView items can only be reliably retrieved on layout.
            val lockScreenPreview =
                (pager.getChildAt(0) as? RecyclerView)
                    ?.findViewHolderForAdapterPosition(LOCK_SCREEN_PREVIEW_POSITION)
                    ?.itemView
            val homeScreenPreview =
                (pager.getChildAt(0) as? RecyclerView)
                    ?.findViewHolderForAdapterPosition(HOME_SCREEN_PREVIEW_POSITION)
                    ?.itemView
            val fadePreview = { position: Int ->
                lockScreenPreview?.apply {
                    findViewById<View>(R.id.wallpaper_surface)
                        .animate()
                        .alpha(
                            if (position == LOCK_SCREEN_PREVIEW_POSITION) ALPHA_SELECTED_PREVIEW
                            else ALPHA_NON_SELECTED_PREVIEW
                        )
                        .setDuration(mediumAnimTimeMs)
                        .start()
                    findViewById<View>(R.id.workspace_surface)
                        .animate()
                        .alpha(
                            if (position == LOCK_SCREEN_PREVIEW_POSITION) ALPHA_SELECTED_PREVIEW
                            else ALPHA_NON_SELECTED_PREVIEW
                        )
                        .setDuration(mediumAnimTimeMs)
                        .start()
                }
                homeScreenPreview?.apply {
                    findViewById<View>(R.id.wallpaper_surface)
                        .animate()
                        .alpha(
                            if (position == HOME_SCREEN_PREVIEW_POSITION) ALPHA_SELECTED_PREVIEW
                            else ALPHA_NON_SELECTED_PREVIEW
                        )
                        .setDuration(mediumAnimTimeMs)
                        .start()
                    findViewById<View>(R.id.workspace_surface)
                        .animate()
                        .alpha(
                            if (position == HOME_SCREEN_PREVIEW_POSITION) ALPHA_SELECTED_PREVIEW
                            else ALPHA_NON_SELECTED_PREVIEW
                        )
                        .setDuration(mediumAnimTimeMs)
                        .start()
                }
            }
            fadePreview(pager.currentItem)
            pager.registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        fadePreview(position)
                    }
                }
            )
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.screen.collect { (screen, option) ->
                        when (screen) {
                            MAIN -> navigateToPrimary()
                            CUSTOMIZATION_OPTION -> option?.let(navigateToSecondary)
                        }
                    }
                }

                launch {
                    viewModel.selectedPreviewScreen.collect {
                        when (it) {
                            LOCK_SCREEN -> {
                                pager.currentItem = 0
                                optionContainer.transitionToStart()
                            }
                            HOME_SCREEN -> {
                                pager.currentItem = 1
                                optionContainer.transitionToEnd()
                            }
                        }
                    }
                }
            }
        }

        customizationOptionsBinder.bind(
            view,
            lockScreenCustomizationOptionEntries,
            homeScreenCustomizationOptionEntries,
            customizationOptionFloatingSheetViewMap,
            viewModel.customizationOptionsViewModel,
            lifecycleOwner,
        )
        return { viewModel.onBackPressed() }
    }
}
