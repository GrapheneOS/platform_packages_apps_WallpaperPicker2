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
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.android.wallpaper.model.PartnerWallpaperInfo
import com.android.wallpaper.model.ThirdPartyLiveWallpaperCategory
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.category.client.DefaultWallpaperCategoryClient
import com.android.wallpaper.picker.category.client.DefaultWallpaperCategoryClientImpl
import com.android.wallpaper.picker.category.client.LiveWallpapersClient
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.category.CommonCategoryData
import com.android.wallpaper.testing.FakeWallpaperParser
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestPartnerProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultWallpaperCategoryClientImplTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var partnerProvider: TestPartnerProvider
    @Inject lateinit var wallpaperXMLParser: FakeWallpaperParser
    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope
    @Inject lateinit var liveWallpapersClient: LiveWallpapersClient

    private lateinit var defaultWallpaperCategoryClient: DefaultWallpaperCategoryClient
    @Inject lateinit var testInjector: TestInjector

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        defaultWallpaperCategoryClient =
            DefaultWallpaperCategoryClientImpl(
                context,
                partnerProvider,
                wallpaperXMLParser,
                liveWallpapersClient
            )
        InjectorProvider.setInjector(testInjector)
        val resources = context.resources
        partnerProvider.resources = resources
        val packageName = context.packageName
        partnerProvider.packageName = packageName
    }

    @Test
    fun getMyPhotosCategory() =
        testScope.runTest {
            val commonCategoryData = CommonCategoryData("My photos", "image_wallpapers", 51)
            val expectedCategoryModel = CategoryModel(commonCategoryData)

            val result = defaultWallpaperCategoryClient.getMyPhotosCategory()

            assertThat(expectedCategoryModel.commonCategoryData.collectionId)
                .isEqualTo(result.collectionId)

            assertThat(expectedCategoryModel.commonCategoryData.priority).isEqualTo(result.priority)

            assertThat(expectedCategoryModel.commonCategoryData.title).isEqualTo(result.title)
        }

    @Test
    fun getValidOnDeviceCategory() =
        testScope.runTest {
            val fakePartnerWallpaperInfo = PartnerWallpaperInfo(1, 1)
            wallpaperXMLParser.wallpapers = listOf(fakePartnerWallpaperInfo)
            val categoryModel =
                async { defaultWallpaperCategoryClient.getOnDeviceCategory() }.await()

            assertThat(categoryModel).isNotNull()
            assertThat(categoryModel?.title).isEqualTo("On-device wallpapers")
            assertThat(categoryModel?.collectionId).isEqualTo("on_device_wallpapers")
        }

    @Test
    fun getNullOnDeviceCategory() =
        testScope.runTest {
            wallpaperXMLParser.wallpapers = emptyList()
            val categoryModel =
                async { defaultWallpaperCategoryClient.getOnDeviceCategory() }.await()

            assertThat(categoryModel).isNull()
        }

    @Test
    fun getThirdPartyLiveWallpaperCategory_withFeatureAndLiveWallpapers_returnsCategory() =
        testScope.runTest {
            val shadowPackageManager = shadowOf(context.packageManager)
            shadowPackageManager.setSystemFeature(PackageManager.FEATURE_LIVE_WALLPAPER, true)

            val excludedPackageNames = emptySet<String>()
            val expectedCategory =
                ThirdPartyLiveWallpaperCategory(
                    "Live wallpapers",
                    "live_wallpapers",
                    liveWallpapersClient.getAll(emptySet()),
                    300,
                    emptySet()
                )

            val result =
                defaultWallpaperCategoryClient.getThirdPartyLiveWallpaperCategory(
                    excludedPackageNames
                )

            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo(expectedCategory.title)
            assertThat(result[0].collectionId).isEqualTo(expectedCategory.collectionId)
        }

    @Test
    fun getSystemCategories() =
        testScope.runTest {
            val categoryModel =
                async { defaultWallpaperCategoryClient.getSystemCategories() }.await()

            assertThat(categoryModel).isNotNull()
            assertThat(categoryModel[0].title).isEqualTo("sample-title-1")
            assertThat(categoryModel[0].collectionId).isEqualTo("sample-collection-id")
        }

    @Test
    fun getThirdPartyCategory() =
        testScope.runTest {
            // Get the shadow package manager
            val shadowPackageManager = shadowOf(context.packageManager)
            val fakeThirdPartyApp1 = createFakeResolveInfo("com.example.app1", "ThirdPartyApp1")
            val fakeThirdPartyApp2 = createFakeResolveInfo("com.example.app2", "ThirdPartyApp2")
            val fakeImagePickerApp = createFakeResolveInfo("com.example.imagepicker", "ImagePicker")
            shadowPackageManager.addResolveInfoForIntent(
                Intent(Intent.ACTION_SET_WALLPAPER),
                listOf(fakeThirdPartyApp1, fakeThirdPartyApp2, fakeImagePickerApp)
            )
            shadowPackageManager.addResolveInfoForIntent(
                Intent(Intent.ACTION_GET_CONTENT).setType("image/*"),
                listOf(fakeImagePickerApp)
            )

            val result = defaultWallpaperCategoryClient.getThirdPartyCategory(emptyList())
            assertThat(result).hasSize(2)
            assertThat(result[0].title).isEqualTo("ThirdPartyApp1")
            assertThat(result[0].collectionId).contains("com.example.app1")
            assertThat(result[1].title).isEqualTo("ThirdPartyApp2")
            assertThat(result[1].collectionId).contains("com.example.app2")
        }

    private fun createFakeResolveInfo(packageName: String, label: String): ResolveInfo {
        return ResolveInfo().apply {
            activityInfo =
                ActivityInfo().apply {
                    this.packageName = packageName
                    name = "${packageName}.MainActivity"
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
