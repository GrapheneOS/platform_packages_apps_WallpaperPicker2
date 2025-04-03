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
 *
 */
package com.android.customization.picker.clock.shared

import android.os.BaseBundle

enum class ClockSize {
    DYNAMIC,
    SMALL;

    companion object {
        /** Tries to extract the preferred clock size of bundle's wallpaper. */
        fun BaseBundle.getPreferredClockSize(): ClockSize? {
            return tryParseClockSize(getString(KEY_PREFERRED_CLOCK_SIZE))
        }

        /** Parses clock size from a bundled value */
        fun tryParseClockSize(targetSize: String?): ClockSize? {
            return when (targetSize) {
                VALUE_PREFERRED_SMALL_CLOCK -> ClockSize.SMALL
                VALUE_PREFERRED_DYNAMIC_CLOCK -> ClockSize.DYNAMIC
                else -> null
            }
        }

        private const val KEY_PREFERRED_CLOCK_SIZE = "PreferredClockSize"
        private const val VALUE_PREFERRED_SMALL_CLOCK = "SMALL"
        private const val VALUE_PREFERRED_DYNAMIC_CLOCK = "DYNAMIC"
    }
}
