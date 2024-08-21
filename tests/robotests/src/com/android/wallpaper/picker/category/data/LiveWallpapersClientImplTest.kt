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

package com.android.wallpaper.picker.category.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.service.wallpaper.WallpaperService
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.category.client.LiveWallpapersClientImpl
import com.android.wallpaper.testing.TestInjector
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LiveWallpapersClientImplTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var testInjector: TestInjector

    private lateinit var liveWallpapersClientImpl: LiveWallpapersClientImpl

    @Before
    fun setup() {
        hiltRule.inject()
        liveWallpapersClientImpl = LiveWallpapersClientImpl(context)
        InjectorProvider.setInjector(testInjector)
    }

    @Test
    fun `test getAllOnDevice returns system wallpapers first`() {
        val systemWallpaperResolveInfo =
            createFakeResolveInfo("com.system.wallpaper", "System Wallpaper")
        val nonSystemWallpaperResolveInfo =
            createFakeResolveInfo("com.non.system.wallpaper", "Non-System Wallpaper")
        val shadowPackageManager = shadowOf(context.packageManager)

        shadowPackageManager.addResolveInfoForIntent(
            Intent(WallpaperService.SERVICE_INTERFACE),
            systemWallpaperResolveInfo
        )

        shadowPackageManager.addResolveInfoForIntent(
            Intent(WallpaperService.SERVICE_INTERFACE),
            nonSystemWallpaperResolveInfo
        )

        val result = liveWallpapersClientImpl.getAllOnDevice()

        assertThat(result.size).isEqualTo(2)
        assertThat(result[0].serviceInfo.packageName)
            .isEqualTo(nonSystemWallpaperResolveInfo.serviceInfo.packageName)
        assertThat(result[1].serviceInfo.packageName)
            .isEqualTo(systemWallpaperResolveInfo.serviceInfo.packageName)
    }

    @Test
    fun `test getAll returns wallpaper infos excluding package names`() {
        val systemWallpaperResolveInfo =
            createFakeResolveInfo("com.system.wallpaper", "System Wallpaper")
        val nonSystemWallpaperResolveInfo =
            createFakeResolveInfo("com.non.system.wallpaper", "Non-System Wallpaper")
        val shadowPackageManager = shadowOf(context.packageManager)

        shadowPackageManager.addResolveInfoForIntent(
            Intent(WallpaperService.SERVICE_INTERFACE),
            systemWallpaperResolveInfo
        )

        shadowPackageManager.addResolveInfoForIntent(
            Intent(WallpaperService.SERVICE_INTERFACE),
            nonSystemWallpaperResolveInfo
        )

        val result =
            liveWallpapersClientImpl.getAll(
                setOf("com.system.wallpaper", "com.non.system.wallpaper")
            )

        assertThat(result.size).isEqualTo(0)
    }

    private fun createFakeResolveInfo(packageName: String, label: String): ResolveInfo {
        return ResolveInfo().apply {
            serviceInfo =
                ServiceInfo().apply {
                    this.packageName = packageName
                    name = "${packageName}.WallpaperService"
                    applicationInfo =
                        ApplicationInfo().apply {
                            this.packageName = packageName
                            labelRes = 0
                            nonLocalizedLabel = label
                        }
                }
        }
    }
}
