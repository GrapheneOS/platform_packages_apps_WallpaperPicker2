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

package com.android.wallpaper.picker.customization.ui.viewmodel

import android.annotation.ColorInt
import android.content.Context
import com.android.customization.picker.mode.data.repository.DarkModeStateRepository
import com.android.systemui.monet.ColorScheme
import com.android.systemui.monet.Style
import com.android.wallpaper.R
import com.google.ux.material.libmonet.dynamiccolor.DynamicColor
import com.google.ux.material.libmonet.dynamiccolor.DynamicScheme
import com.google.ux.material.libmonet.dynamiccolor.MaterialDynamicColors
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.lifecycle.RetainedLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@ActivityRetainedScoped
class ColorUpdateViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    activityRetainedLifecycle: ActivityRetainedLifecycle,
    private val darkModeStateRepository: DarkModeStateRepository,
) {
    private val coroutineScope = RetainedLifecycleCoroutineScope(activityRetainedLifecycle)

    private val isPreviewEnabled = MutableStateFlow(false)

    /**
     * Flow that emits an event whenever the system colors are updated. This flow has a replay of 1,
     * so it will emit the last event to new subscribers.
     */
    private val _systemColorsUpdated: MutableSharedFlow<Unit> =
        MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }
    val systemColorsUpdated = _systemColorsUpdated.asSharedFlow()

    /**
     * Flow that emits an event whenever the system colors are updated. This flow does not have a
     * replay, so it will not emit the last event to new subscribers.
     */
    private val _systemColorsUpdatedNoReplay: MutableSharedFlow<Unit> = MutableSharedFlow()
    val systemColorsUpdatedNoReplay = _systemColorsUpdatedNoReplay.asSharedFlow()

    private val previewingIsDarkMode: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private val systemIsDarkMode = darkModeStateRepository.isDarkMode
    val isDarkMode =
        combine(previewingIsDarkMode, systemIsDarkMode, isPreviewEnabled) {
                previewingIsDarkMode,
                systemIsDarkMode,
                isPreviewEnabled ->
                if (previewingIsDarkMode != null && isPreviewEnabled) previewingIsDarkMode
                else systemIsDarkMode
            }
            .distinctUntilChanged()

    private val previewingColorScheme: MutableStateFlow<DynamicScheme?> = MutableStateFlow(null)
    private val colors: MutableList<Color> = mutableListOf()

    private inner class Color(private val colorResId: Int, dynamicColor: DynamicColor) {
        private val color = MutableStateFlow(context.getColor(colorResId))
        val colorFlow =
            combine(color, previewingColorScheme, isPreviewEnabled) {
                systemColor,
                previewScheme,
                isEnabled ->
                if (previewScheme != null && isEnabled) {
                    previewScheme.getArgb(dynamicColor)
                } else systemColor
            }

        fun update() {
            color.value = context.getColor(colorResId)
        }
    }

    private fun createColorFlow(colorResId: Int, dynamicColor: DynamicColor): Flow<Int> {
        val color = Color(colorResId, dynamicColor)
        colors.add(color)
        return color.colorFlow
    }

    val colorPrimary = createColorFlow(R.color.system_primary, MaterialDynamicColors().primary())
    val colorPrimaryContainer =
        createColorFlow(
            R.color.system_primary_container,
            MaterialDynamicColors().primaryContainer(),
        )
    val colorPrimaryFixedDim =
        createColorFlow(R.color.system_primary_fixed_dim, MaterialDynamicColors().primaryFixedDim())
    val colorOnPrimary =
        createColorFlow(R.color.system_on_primary, MaterialDynamicColors().onPrimary())
    val colorOnPrimaryContainer =
        createColorFlow(
            R.color.system_on_primary_container,
            MaterialDynamicColors().onPrimaryContainer(),
        )
    val colorOnPrimaryFixed =
        createColorFlow(R.color.system_on_primary_fixed, MaterialDynamicColors().onPrimaryFixed())
    val colorOnPrimaryFixedVariant =
        createColorFlow(
            R.color.system_on_primary_fixed_variant,
            MaterialDynamicColors().onPrimaryFixedVariant(),
        )
    val colorSecondary =
        createColorFlow(R.color.system_secondary, MaterialDynamicColors().secondary())
    val colorSecondaryContainer =
        createColorFlow(
            R.color.system_secondary_container,
            MaterialDynamicColors().secondaryContainer(),
        )
    val colorOnSecondaryContainer =
        createColorFlow(
            R.color.system_on_secondary_container,
            MaterialDynamicColors().onSecondaryContainer(),
        )
    val colorSurfaceContainer =
        createColorFlow(
            R.color.system_surface_container,
            MaterialDynamicColors().surfaceContainer(),
        )
    val colorOnSurface =
        createColorFlow(R.color.system_on_surface, MaterialDynamicColors().onSurface())
    val colorOnSurfaceVariant =
        createColorFlow(
            R.color.system_on_surface_variant,
            MaterialDynamicColors().onSurfaceVariant(),
        )
    val colorSurfaceContainerHigh =
        createColorFlow(
            R.color.system_surface_container_high,
            MaterialDynamicColors().surfaceContainerHigh(),
        )
    val colorSurfaceContainerHighest =
        createColorFlow(
            R.color.system_surface_container_highest,
            MaterialDynamicColors().surfaceContainerHighest(),
        )
    val colorSurfaceBright =
        createColorFlow(R.color.system_surface_bright, MaterialDynamicColors().surfaceBright())
    val colorOutline = createColorFlow(R.color.system_outline, MaterialDynamicColors().outline())

    // Custom day/night color pairing
    val floatingToolbarBackground =
        createColorFlow(
            R.color.floating_toolbar_background,
            if (!context.resources.configuration.isNightModeActive) {
                MaterialDynamicColors().surfaceBright()
            } else {
                MaterialDynamicColors().surfaceContainerHigh()
            },
        )

    fun previewColors(@ColorInt colorSeed: Int, @Style.Type style: Int, isDarkMode: Boolean) {
        previewingColorScheme.value = ColorScheme(colorSeed, isDarkMode, style).materialScheme
        previewingIsDarkMode.value = isDarkMode
    }

    fun resetPreview() {
        previewingColorScheme.value = null
        previewingIsDarkMode.value = null
    }

    fun setPreviewEnabled(isEnabled: Boolean) {
        isPreviewEnabled.value = isEnabled
    }

    fun updateColors() {
        // Launch a coroutine scope and use emit rather than tryEmit to make sure the update always
        // emits successfully.
        coroutineScope.launch {
            _systemColorsUpdated.emit(Unit)
            _systemColorsUpdatedNoReplay.emit(Unit)
        }
        colors.forEach { it.update() }
    }

    fun updateDarkModeAndColors() {
        darkModeStateRepository.refreshIsDarkMode()
        // Colors always need an update when dark mode is updated
        updateColors()
    }

    class RetainedLifecycleCoroutineScope(val lifecycle: RetainedLifecycle) :
        CoroutineScope, RetainedLifecycle.OnClearedListener {

        override val coroutineContext: CoroutineContext =
            SupervisorJob() + Dispatchers.Main.immediate

        init {
            lifecycle.addOnClearedListener(this)
        }

        override fun onCleared() {
            coroutineContext.cancel()
        }
    }
}
