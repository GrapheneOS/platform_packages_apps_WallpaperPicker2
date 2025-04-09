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

package com.android.wallpaper.picker.category.ui.binder

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

/** This is the binder interface for providing custom banner views. */
interface BannerProvider {
    fun getSignInBanner(): View?

    fun getIcon(view: View?): ImageView?

    fun getDismissButton(view: View?): Button?

    fun getSignInButton(view: View?): Button?

    fun getBannerTitle(view: View?): TextView?

    fun getBannerDescription(view: View?): TextView?
}
