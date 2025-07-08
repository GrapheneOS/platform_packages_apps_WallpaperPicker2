/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.testing

import android.content.ContentResolver
import android.net.Uri
import com.android.wallpaper.util.BasePreviewUtils

class FakePreviewUtils : BasePreviewUtils {
    override fun getUri(path: String?): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(FAKE_AUTHORITY)
            .appendPath(path)
            .build()
    }

    private var supportsPreview = true

    override fun supportsPreview(): Boolean {
        return supportsPreview
    }

    fun setSupportsPreview(supportsPreview: Boolean) {
        this.supportsPreview = supportsPreview
    }

    companion object {
        const val FAKE_AUTHORITY = "com.example.fake"
    }
}
