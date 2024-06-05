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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.picker.customization.ui.CustomizationPickerActivity2
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen.CUSTOMIZATION_OPTION
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2.PickerScreen.MAIN
import kotlinx.coroutines.launch

object CustomizationPickerBinder2 {

    /**
     * @return Callback for the [CustomizationPickerActivity2] to set
     *   [CustomizationPickerViewModel2]'s screen state to null, which infers to the main screen. We
     *   need this callback to handle the back navigation in [CustomizationPickerActivity2].
     */
    fun bind(
        view: View,
        viewModel: CustomizationPickerViewModel2,
        customizationOptionsBinder: CustomizationOptionsBinder,
        lifecycleOwner: LifecycleOwner,
        navigateToPrimary: () -> Unit,
        navigateToSecondary: (screen: CustomizationOptionUtil.CustomizationOption) -> Unit,
    ): () -> Boolean {
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
            }
        }

        customizationOptionsBinder.bind(
            view,
            viewModel.customizationOptionsViewModel,
            lifecycleOwner,
        )
        return { viewModel.onBackPressed() }
    }
}
