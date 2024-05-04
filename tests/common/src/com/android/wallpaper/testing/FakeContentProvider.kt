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

package com.android.wallpaper.testing

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.android.wallpaper.effects.EffectContract
import com.android.wallpaper.effects.FakeEffectsController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeContentProvider @Inject constructor() : ContentProvider() {
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor =
            MatrixCursor(
                arrayOf(
                    EffectContract.ASSET_ID,
                    EffectContract.KEY_EFFECT_ID,
                    EffectContract.KEY_EFFECT_TITLE,
                ),
            )
        if (uri.authority.equals(FakeEffectsController.AUTHORITY)) {
            // Return to-be-installed component names (flatten String)
            cursor
                .newRow()
                .add(EffectContract.ASSET_ID, FAKE_ASSET_ID)
                .add(EffectContract.KEY_EFFECT_ID, FAKE_EFFECT_ID)
                .add(EffectContract.KEY_EFFECT_TITLE, FAKE_EFFECT_TITLE)
        }
        return cursor
    }

    override fun getType(uri: Uri): String? {
        TODO("Not yet implemented")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        TODO("Not yet implemented")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        TODO("Not yet implemented")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        TODO("Not yet implemented")
    }

    override fun onCreate(): Boolean {
        TODO("Not yet implemented")
    }

    companion object {
        const val FAKE_ASSET_ID = 1
        const val FAKE_EFFECT_ID = 1
        const val FAKE_EFFECT_TITLE = "Fake Effect Title"
    }
}
