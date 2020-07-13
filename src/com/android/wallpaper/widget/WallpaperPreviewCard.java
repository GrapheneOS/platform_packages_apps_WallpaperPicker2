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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.android.wallpaper.R;
import com.android.wallpaper.util.ScreenSizeCalculator;

/**
 * A Widget consists of a CardView to show proper preview based on screen aspect ratio.
 */
public class WallpaperPreviewCard extends LinearLayout {

    private float mScreenAspectRatio;

    public WallpaperPreviewCard(Context context) {
        this(context, null);
    }

    public WallpaperPreviewCard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WallpaperPreviewCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.wallpaper_preview_card_layout, this);
        mScreenAspectRatio = ScreenSizeCalculator.getInstance().getScreenAspectRatio(getContext());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredViewHeight = this.getMeasuredHeight();
        int measuredViewWidth = this.getMeasuredWidth();
        int absoluteViewWidth = (int) ((measuredViewHeight - this.getPaddingBottom()
                - this.getPaddingTop()) / mScreenAspectRatio);
        int horizontalPadding = (measuredViewWidth - absoluteViewWidth) / 2;
        this.setPaddingRelative(
                horizontalPadding,
                this.getPaddingTop(),
                horizontalPadding,
                this.getPaddingBottom());
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
