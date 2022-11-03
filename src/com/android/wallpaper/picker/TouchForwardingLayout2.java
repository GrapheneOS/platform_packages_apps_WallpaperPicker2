/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.wallpaper.picker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * A frame layout that listens to touch events and routes them to another view.
 * TODO b/256819974 This is a duplicate of TouchForwardingLayout and should be removed after
 * TouchForwardingLayout in the branch of tm-qpr-dev is synced with master.
 */
public class TouchForwardingLayout2 extends FrameLayout {

    private View mView;
    private boolean mForwardingEnabled;

    public TouchForwardingLayout2(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mView != null && mForwardingEnabled) {
            mView.dispatchTouchEvent(ev);
        }
        return true;
    }

    /** Set the view that the touch events are routed to */
    public void setTargetView(View view) {
        mView = view;
    }

    public void setForwardingEnabled(boolean forwardingEnabled) {
        mForwardingEnabled = forwardingEnabled;
    }
}
