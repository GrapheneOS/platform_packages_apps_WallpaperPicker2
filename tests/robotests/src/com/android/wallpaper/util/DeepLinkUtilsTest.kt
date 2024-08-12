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
package com.android.wallpaper.util

import android.content.Intent
import android.net.Uri
import com.android.wallpaper.util.DeepLinkUtils.EXTRA_KEY_COLLECTION_ID
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@HiltAndroidTest
class DeepLinkUtilsTest {
    private lateinit var intent: Intent

    @Before
    fun setUp() {
        intent = Intent()
    }

    @Test
    fun testIsDeepLink_DeeplinkIntent_returnsTrue() {
        intent.data = Uri.fromParts("https", "//g.co/wallpaper", "foo")
        assertThat(DeepLinkUtils.isDeepLink(intent)).isTrue()
    }

    @Test
    fun testIsDeepLink_NoData_returnsFalse() {
        assertThat(DeepLinkUtils.isDeepLink(intent)).isFalse()
    }

    @Test
    fun testIsDeepLink_FakeDomainUri_returnsFalse() {
        intent.data = Uri.fromParts("https", "//example.com", "foo")
        assertThat(DeepLinkUtils.isDeepLink(intent)).isFalse()
    }

    @Test
    fun getCollectionId_FromUri() {
        val testCollection = "test_collection"
        intent.data = Uri.parse("https://g.co/wallpaper?collection_id=$testCollection")
        assertThat(DeepLinkUtils.getCollectionId(intent)).isEqualTo(testCollection)
    }

    @Test
    fun getCollectionId_FromExtra() {
        val testCollection = "test_collection"
        intent.putExtra(EXTRA_KEY_COLLECTION_ID, testCollection)
        assertThat(DeepLinkUtils.getCollectionId(intent)).isEqualTo(testCollection)
    }

    @Test
    fun getCollectionId_Empty() {
        assertThat(DeepLinkUtils.getCollectionId(intent)).isNull()
    }
}
