/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.wallpaper.module

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SET_WALLPAPER
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.provider.Settings.*

/**
 * Utility class to check the support of two panes integration (trampoline)
 */
class LargeScreenTwoPanesChecker : TwoPanesChecker {
    companion object {
        private const val TAG = "LargeScreenTwoPanesChecker"
        private const val VALUE_HIGHLIGHT_MENU = "top_level_wallpaper"
    }

    override fun isTwoPanesEnabled(context: Context): Boolean {
        val pm = context.packageManager
        val intent = getTwoPanesIntent(context)

        val resolveInfo = pm.resolveActivity(intent, MATCH_DEFAULT_ONLY)?.activityInfo?.enabled
        return resolveInfo != null
    }

    override fun getTwoPanesIntent(context: Context): Intent {
        val intentUri = Intent(ACTION_SET_WALLPAPER)
                .setPackage(context.packageName).toUri(Intent.URI_INTENT_SCHEME)

        return Intent(ACTION_SETTINGS_LARGE_SCREEN_DEEP_LINK).apply {
            putExtra(EXTRA_SETTINGS_LARGE_SCREEN_HIGHLIGHT_MENU_KEY, VALUE_HIGHLIGHT_MENU)
            putExtra(EXTRA_SETTINGS_LARGE_SCREEN_DEEP_LINK_INTENT_URI, intentUri)
        };
    }
}
