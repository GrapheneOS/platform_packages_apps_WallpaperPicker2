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
package com.android.wallpaper.modules

import com.android.wallpaper.effects.DefaultEffectsController
import com.android.wallpaper.effects.EffectsController
import com.android.wallpaper.module.DefaultPartnerProvider
import com.android.wallpaper.module.DefaultWallpaperPreferences
import com.android.wallpaper.module.Injector
import com.android.wallpaper.module.PartnerProvider
import com.android.wallpaper.module.WallpaperPicker2Injector
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.module.logging.NoOpUserEventLogger
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.category.domain.interactor.CategoriesLoadingStatusInteractor
import com.android.wallpaper.picker.category.domain.interactor.CategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.CreativeCategoryInteractor
import com.android.wallpaper.picker.category.domain.interactor.implementations.CategoryInteractorImpl
import com.android.wallpaper.picker.category.domain.interactor.implementations.CreativeCategoryInteractorImpl
import com.android.wallpaper.picker.category.domain.interactor.implementations.DefaultCategoriesLoadingStatusInteractor
import com.android.wallpaper.picker.category.ui.view.providers.IndividualPickerFactory
import com.android.wallpaper.picker.category.ui.view.providers.implementation.DefaultIndividualPickerFactory
import com.android.wallpaper.picker.category.wrapper.DefaultWallpaperCategoryWrapper
import com.android.wallpaper.picker.category.wrapper.WallpaperCategoryWrapper
import com.android.wallpaper.picker.common.preview.ui.binder.DefaultWorkspaceCallbackBinder
import com.android.wallpaper.picker.common.preview.ui.binder.WorkspaceCallbackBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultCustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultToolbarBinder
import com.android.wallpaper.picker.customization.ui.binder.ToolbarBinder
import com.android.wallpaper.picker.preview.ui.util.DefaultImageEffectDialogUtil
import com.android.wallpaper.picker.preview.ui.util.ImageEffectDialogUtil
import com.android.wallpaper.util.converter.DefaultWallpaperModelFactory
import com.android.wallpaper.util.converter.WallpaperModelFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WallpaperPicker2AppModule {

    @Binds
    @Singleton
    abstract fun bindCreativeCategoryInteractor(
        impl: CreativeCategoryInteractorImpl
    ): CreativeCategoryInteractor

    @Binds
    @Singleton
    abstract fun bindCustomizationOptionsBinder(
        impl: DefaultCustomizationOptionsBinder
    ): CustomizationOptionsBinder

    @Binds
    @Singleton
    abstract fun bindEffectsController(impl: DefaultEffectsController): EffectsController

    @Binds
    @Singleton
    abstract fun bindGoogleCategoryInteractor(impl: CategoryInteractorImpl): CategoryInteractor

    @Binds
    @Singleton
    abstract fun bindImageEffectDialogUtil(
        impl: DefaultImageEffectDialogUtil
    ): ImageEffectDialogUtil

    @Binds
    @Singleton
    abstract fun bindIndividualPickerFactory(
        impl: DefaultIndividualPickerFactory
    ): IndividualPickerFactory

    @Binds @Singleton abstract fun bindInjector(impl: WallpaperPicker2Injector): Injector

    @Binds
    @Singleton
    abstract fun bindLoadingStatusInteractor(
        impl: DefaultCategoriesLoadingStatusInteractor
    ): CategoriesLoadingStatusInteractor

    @Binds
    @Singleton
    abstract fun bindPartnerProvider(impl: DefaultPartnerProvider): PartnerProvider

    @Binds @Singleton abstract fun bindToolbarBinder(impl: DefaultToolbarBinder): ToolbarBinder

    @Binds
    @Singleton
    abstract fun bindWallpaperCategoryWrapper(
        impl: DefaultWallpaperCategoryWrapper
    ): WallpaperCategoryWrapper

    @Binds
    @Singleton
    abstract fun bindWallpaperModelFactory(
        impl: DefaultWallpaperModelFactory
    ): WallpaperModelFactory

    @Binds
    @Singleton
    abstract fun bindWallpaperPreferences(impl: DefaultWallpaperPreferences): WallpaperPreferences

    @Binds
    @Singleton
    abstract fun bindWorkspaceCallbackBinder(
        impl: DefaultWorkspaceCallbackBinder
    ): WorkspaceCallbackBinder

    companion object {

        @Provides
        @Singleton
        fun provideUserEventLogger(): UserEventLogger {
            return NoOpUserEventLogger()
        }
    }
}
