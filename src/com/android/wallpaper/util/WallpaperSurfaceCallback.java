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
package com.android.wallpaper.util;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.wallpaper.R;

/**
 * Default implementation of {@link SurfaceHolder.Callback} to render a static wallpaper when the
 * surface has been created.
 */
public class WallpaperSurfaceCallback implements SurfaceHolder.Callback {

    /**
     * Listener used to be notified when this surface is created
     */
    public interface SurfaceListener {
        /**
         * Called when {@link WallpaperSurfaceCallback#surfaceCreated(SurfaceHolder)} is called.
         */
        void onSurfaceCreated();
    }

    private Surface mLastSurface;
    private SurfaceControlViewHost mHost;
    // Home workspace surface is behind the app window, and so must the home image wallpaper like
    // the live wallpaper. This view is rendered on here for home image wallpaper.
    private ImageView mHomeImageWallpaper;
    private final Context mContext;
    private final ImageView mHomePreview;
    private final SurfaceView mWallpaperSurface;
    @Nullable
    private final SurfaceListener mListener;

    public WallpaperSurfaceCallback(Context context, ImageView homePreview,
            SurfaceView wallpaperSurface, @Nullable  SurfaceListener listener) {
        mContext = context;
        mHomePreview = homePreview;
        mWallpaperSurface = wallpaperSurface;
        mListener = listener;
    }

    public WallpaperSurfaceCallback(Context context, ImageView homePreview,
            SurfaceView wallpaperSurface) {
        this(context, homePreview, wallpaperSurface, null);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mLastSurface != holder.getSurface()) {
            mLastSurface = holder.getSurface();
            mHomeImageWallpaper = new ImageView(mContext);
            mHomeImageWallpaper.setBackgroundColor(
                    ContextCompat.getColor(mContext, R.color.primary_color));
            mHomeImageWallpaper.measure(makeMeasureSpec(mHomePreview.getWidth(), EXACTLY),
                    makeMeasureSpec(mHomePreview.getHeight(), EXACTLY));
            mHomeImageWallpaper.layout(0, 0, mHomePreview.getWidth(),
                    mHomePreview.getHeight());

            cleanUp();
            mHost = new SurfaceControlViewHost(mContext,
                    mContext.getDisplay(), mWallpaperSurface.getHostToken());
            mHost.setView(mHomeImageWallpaper, mHomeImageWallpaper.getWidth(),
                    mHomeImageWallpaper.getHeight());
            mWallpaperSurface.setChildSurfacePackage(mHost.getSurfacePackage());
        }
        if (mListener != null) {
            mListener.onSurfaceCreated();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { }

    /**
     * Call to release resources.
     */
    public void cleanUp() {
        if (mHost != null) {
            mHost.release();
            mHost = null;
        }
    }

    @Nullable
    public ImageView getHomeImageWallpaper() {
        return mHomeImageWallpaper;
    }
}
