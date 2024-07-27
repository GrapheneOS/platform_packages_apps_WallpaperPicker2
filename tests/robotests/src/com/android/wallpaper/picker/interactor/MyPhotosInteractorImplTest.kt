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

package com.android.wallpaper.picker.interactor

import android.content.Context
import com.android.wallpaper.picker.category.domain.interactor.implementations.MyPhotosInteractorImpl
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.testing.FakeDefaultWallpaperCategoryRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MyPhotosInteractorImplTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject @ApplicationContext lateinit var context: Context

    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope
    @Inject
    lateinit var fakeDefaultWallpaperCategoryRepository: FakeDefaultWallpaperCategoryRepository
    private lateinit var myPhotosInteractorImpl: MyPhotosInteractorImpl

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        myPhotosInteractorImpl =
            MyPhotosInteractorImpl(fakeDefaultWallpaperCategoryRepository, testScope)
    }

    @Test
    fun `category flow emits correct values`() = runTest {
        fakeDefaultWallpaperCategoryRepository.fetchMyPhotosCategory()

        val emittedCategories = mutableListOf<CategoryModel>()
        val job = launch { myPhotosInteractorImpl.category.collect { emittedCategories.add(it) } }

        // Wait for the collection to happen
        advanceUntilIdle()
        job.cancel()
        assertThat(emittedCategories[0].commonCategoryData.title).isEqualTo("Fake My Photos")
    }
}
