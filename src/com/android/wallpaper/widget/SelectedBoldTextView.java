/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.wallpaper.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.android.wallpaper.R;

/** A TextView that would update the text style to bold when selected. */
public class SelectedBoldTextView extends AppCompatTextView {

    public SelectedBoldTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setTextColor(context.getColorStateList(R.color.full_preview_tab_text_color));
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }
}
