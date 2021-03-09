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
package com.android.wallpaper.picker.individual;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * A view where the image can be optionally clipped to have a circular border.
 */
public class CircularImageView extends ImageView {
    private boolean mClipped = false;

    private boolean mPathSet = false;
    private Path mPath;

    public CircularImageView(Context context) {
        super(context);
    }

    public CircularImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircularImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Returns whether the image is clipped with a circular boundary.
     */
    public boolean getClipped() {
        return mClipped;
    }

    /**
     * Modifies how the image is clipped. When called with true, the image
     * is clipped with a circular boundary; with false, the default boundary.
     *
     * @param clippedValue Whether the image is clipped with a circular
     *                     boundary.
     */
    public void setClipped(boolean clippedValue) {
        mClipped = clippedValue;
        invalidate();
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mClipped) {
            if (!mPathSet) {
                // Computes path.
                mPath = new Path();
                mPath.addCircle(
                        getWidth() / 2,
                        getHeight() / 2,
                        getHeight() / 2,
                        Path.Direction.CW);
                mPathSet = true;
            }
            canvas.clipPath(mPath);
        }

        super.onDraw(canvas);
    }
}

