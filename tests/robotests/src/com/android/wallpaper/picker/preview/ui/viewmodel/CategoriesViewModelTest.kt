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

package com.android.wallpaper.picker.preview.ui.viewmodel

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.activity.viewModels
import androidx.test.core.app.ActivityScenario
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel
import com.android.wallpaper.picker.preview.PreviewTestActivity
import com.android.wallpaper.testing.TestInjector
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CategoriesViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var scenario: ActivityScenario<PreviewTestActivity>
    private lateinit var categoriesViewModel: CategoriesViewModel

    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject @ApplicationContext lateinit var appContext: Context

    @Inject lateinit var testInjector: TestInjector

    @Before
    fun setUp() {
        hiltRule.inject()

        InjectorProvider.setInjector(testInjector)
        Dispatchers.setMain(testDispatcher)

        val activityInfo =
            ActivityInfo().apply {
                name = PreviewTestActivity::class.java.name
                packageName = appContext.packageName
            }
        Shadows.shadowOf(appContext.packageManager).addOrUpdateActivity(activityInfo)
        scenario = ActivityScenario.launch(PreviewTestActivity::class.java)
        scenario.onActivity { setEverything(it) }
    }

    private fun setEverything(activity: PreviewTestActivity) {
        categoriesViewModel = activity.viewModels<CategoriesViewModel>().value
    }

    // Studio requires at least one test or else it will report a failure
    @Test
    fun generateTiles_succeeds() {
        assertThat(categoriesViewModel.sections).isNotNull()
    }

    // TODO (b/343476732): add test cases when [CategoriesViewModel] is ready
}
