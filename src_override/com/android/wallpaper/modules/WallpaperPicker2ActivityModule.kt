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

package com.android.wallpaper.modules

import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.clock.ui.view.DefaultClockViewFactory
import com.android.customization.picker.icon.ui.util.DefaultIconStyleViewUtil
import com.android.customization.picker.icon.ui.util.IconStyleViewUtil
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionViewUtil
import com.android.wallpaper.picker.customization.ui.util.DefaultCustomizationOptionViewUtil
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
abstract class WallpaperPicker2ActivityModule {

    @Binds
    @ActivityScoped
    abstract fun bindClockViewFactory(impl: DefaultClockViewFactory): ClockViewFactory

    @Binds
    @ActivityScoped
    abstract fun bindCustomizationOptionViewUtil(
        impl: DefaultCustomizationOptionViewUtil
    ): CustomizationOptionViewUtil

    @Binds
    @ActivityScoped
    abstract fun bindIconStyleViewUtil(impl: DefaultIconStyleViewUtil): IconStyleViewUtil
}
