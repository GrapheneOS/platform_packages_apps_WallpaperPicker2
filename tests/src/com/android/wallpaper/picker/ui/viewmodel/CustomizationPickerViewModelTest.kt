/*
 * Copyright (C) 2022 The Android Open Source Project
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
 *
 */

package com.android.wallpaper.picker.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(JUnit4::class)
class CustomizationPickerViewModelTest {

    private lateinit var underTest: CustomizationPickerViewModel

    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        savedStateHandle = SavedStateHandle()
        underTest =
            CustomizationPickerViewModel(
                savedStateHandle = savedStateHandle,
            )
    }

    @Test
    fun `initial tab is lock screen`() = runTest {
        val homeScreenTab = collectLastValue(underTest.homeScreenTab)
        val lockScreenTab = collectLastValue(underTest.lockScreenTab)
        val isOnLockScreen = collectLastValue(underTest.isOnLockScreen)

        assertThat(homeScreenTab()?.isSelected).isFalse()
        assertThat(lockScreenTab()?.isSelected).isTrue()
        assertThat(isOnLockScreen()).isTrue()
    }

    @Test
    fun `switching to the home screen`() = runTest {
        val homeScreenTab = collectLastValue(underTest.homeScreenTab)
        val lockScreenTab = collectLastValue(underTest.lockScreenTab)
        val isOnLockScreen = collectLastValue(underTest.isOnLockScreen)

        homeScreenTab()?.onClicked?.invoke()

        assertThat(homeScreenTab()?.isSelected).isTrue()
        assertThat(lockScreenTab()?.isSelected).isFalse()
        assertThat(isOnLockScreen()).isFalse()
    }

    @Test
    fun `switching to the home screen and back to the lock screen`() = runTest {
        val homeScreenTab = collectLastValue(underTest.homeScreenTab)
        val lockScreenTab = collectLastValue(underTest.lockScreenTab)
        val isOnLockScreen = collectLastValue(underTest.isOnLockScreen)

        homeScreenTab()?.onClicked?.invoke()
        lockScreenTab()?.onClicked?.invoke()

        assertThat(homeScreenTab()?.isSelected).isFalse()
        assertThat(lockScreenTab()?.isSelected).isTrue()
        assertThat(isOnLockScreen()).isTrue()
    }

    @Test
    fun `restores saved state`() = runTest {
        val oldHomeScreenTab = collectLastValue(underTest.homeScreenTab)

        // Switch to the home screen, which is **not** the default.
        oldHomeScreenTab()?.onClicked?.invoke()

        // Instantiate a new view-model with the same saved state
        val newUnderTest = CustomizationPickerViewModel(savedStateHandle = savedStateHandle)
        val newHomeScreenTab = collectLastValue(newUnderTest.homeScreenTab)

        assertThat(newHomeScreenTab()?.isSelected).isTrue()
    }

    /** Collect [flow] in a new [Job] and return a getter for the last collected value. */
    private fun <T> TestScope.collectLastValue(
        flow: Flow<T>,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
    ): () -> T? {
        var lastValue: T? = null
        backgroundScope.launch(context, start) { flow.collect { lastValue = it } }
        return {
            runCurrent()
            lastValue
        }
    }
}
