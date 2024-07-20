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

package com.android.wallpaper.picker.category.repository

import android.content.Context
import com.android.wallpaper.model.Category
import com.android.wallpaper.model.ImageCategory
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.category.data.repository.DefaultWallpaperCategoryRepository
import com.android.wallpaper.testing.FakeDefaultCategoryFactory
import com.android.wallpaper.testing.FakeDefaultWallpaperCategoryClient
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestStaticWallpaperInfo
import com.android.wallpaper.testing.TestWallpaperCategory
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultWallpaperCategoryRepositoryTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var defaultCategoryFactory: FakeDefaultCategoryFactory
    @Inject lateinit var defaultWallpaperCategoryClient: FakeDefaultWallpaperCategoryClient
    @Inject lateinit var testScope: TestScope
    @Inject lateinit var testInjector: TestInjector

    lateinit var repository: DefaultWallpaperCategoryRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        InjectorProvider.setInjector(testInjector)
    }

    @Test
    fun `fetchAllCategories should update categories and set isAllCategoriesFetched to true`() =
        runTest {
            val category1: Category =
                ImageCategory(
                    "My photos" /* title */,
                    "image_wallpapers" /* collection */,
                    0 /* priority */
                )

            val wallpapers = ArrayList<WallpaperInfo>()
            val wallpaperInfo: WallpaperInfo = TestStaticWallpaperInfo(0)
            wallpapers.add(wallpaperInfo)
            val category2: Category =
                TestWallpaperCategory(
                    "Test category",
                    "init_collection",
                    wallpapers,
                    1 /* priority */
                )

            val mCategories = ArrayList<Category>()
            mCategories.add(category1)
            mCategories.add(category2)

            defaultWallpaperCategoryClient.setSystemCategories(mCategories)

            repository =
                DefaultWallpaperCategoryRepository(
                    context,
                    defaultWallpaperCategoryClient,
                    defaultCategoryFactory,
                    testScope
                )
            testScope.advanceUntilIdle()
            assertThat(repository.isDefaultCategoriesFetched.value).isTrue()
            assertThat(repository.systemCategories).isNotNull()
        }

    @Test
    fun initialStateShouldBeEmpty() = runTest {
        repository =
            DefaultWallpaperCategoryRepository(
                context,
                defaultWallpaperCategoryClient,
                defaultCategoryFactory,
                testScope
            )
        assertThat(repository.systemCategories.value).isEmpty()
        assertThat(repository.isDefaultCategoriesFetched.value).isFalse()
    }
}
