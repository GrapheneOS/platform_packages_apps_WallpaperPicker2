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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toolbar
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

@Singleton
class DefaultToolbarBinder @Inject constructor() : ToolbarBinder {

    override fun bind(
        navButton: FrameLayout,
        toolbar: Toolbar,
        applyButton: Button,
        viewModel: CustomizationOptionsViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        val appContext = navButton.context.applicationContext
        val navButtonIcon = navButton.requireViewById<View>(R.id.nav_button_icon)
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedOption.collect {
                        if (it == null) {
                            navButtonIcon.background =
                                AppCompatResources.getDrawable(
                                    appContext,
                                    R.drawable.ic_arrow_back_24dp
                                )
                        } else {
                            navButtonIcon.background =
                                AppCompatResources.getDrawable(appContext, R.drawable.ic_close_24dp)
                            navButtonIcon.setOnClickListener { viewModel.deselectOption() }
                        }
                    }
                }
            }
        }
    }
}
