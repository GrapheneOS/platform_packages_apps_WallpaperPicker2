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
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.picker.customization.ui.CustomizationPickerActivity2
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen.CLOCK
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen.MAIN
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen.SHORTCUT
import kotlinx.coroutines.launch

object CustomizationPickerBinder2 {

    /**
     * @return Callback for the [CustomizationPickerActivity2] to set
     *   [CustomizationPickerViewModel2]'s screen state to [MAIN]. This is needed since we handle
     *   the back navigation in [CustomizationPickerActivity2] to go back to the main screen.
     */
    fun bind(
        view: View,
        viewModel: CustomizationPickerViewModel2,
        lifecycleOwner: LifecycleOwner,
        navigateToPrimary: () -> Unit,
        navigateToSecondary: (screen: PickerScreen) -> Unit,
    ): () -> Boolean {
        val optionClock = view.requireViewById<TextView>(R.id.option_clock)
        val optionShortcut = view.requireViewById<TextView>(R.id.option_shortcut)
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.onCustomizeClockClicked.collect {
                        optionClock.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    viewModel.onCustomizeShortcutClicked.collect {
                        optionShortcut.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    viewModel.screen.collect {
                        when (it) {
                            MAIN -> navigateToPrimary()
                            CLOCK,
                            SHORTCUT -> navigateToSecondary(it)
                        }
                    }
                }
            }
        }
        return { viewModel.setScreenStateMain() }
    }
}
