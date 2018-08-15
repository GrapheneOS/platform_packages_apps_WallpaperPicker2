/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.android.wallpaper.R;
import com.android.wallpaper.compat.ButtonDrawableSetterCompat;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog fragment which shows the "Set wallpaper" destination dialog for N+ devices. Lets user
 * choose whether to set the wallpaper on the home screen, lock screen, or both.
 */
public class SetWallpaperDialogFragment extends DialogFragment {

    private Button mSetHomeWallpaperButton;
    private Button mSetLockWallpaperButton;
    private Button mSetBothWallpaperButton;

    private boolean mHomeAvailable = true;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        Context context = getContext();

        int titleResId = R.string.set_wallpaper_dialog_message;
        final Listener callback = (Listener) getTargetFragment();

        @SuppressWarnings("RestrictTo")
        View layout =
                View.inflate(
                        new ContextThemeWrapper(getActivity(), R.style.LightDialogTheme),
                        R.layout.dialog_set_wallpaper,
                        null);

        AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.LightDialogTheme)
                .setTitle(titleResId)
                .setView(layout)
                .create();

        mSetHomeWallpaperButton = layout.findViewById(R.id.set_home_wallpaper_button);
        mSetHomeWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onSetHomeScreen();
                dismiss();
            }
        });
        ButtonDrawableSetterCompat.setDrawableToButtonStart(
                mSetHomeWallpaperButton,
                context.getDrawable(R.drawable.ic_home_24px));

        mSetLockWallpaperButton = layout.findViewById(R.id.set_lock_wallpaper_button);
        mSetLockWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onSetLockScreen();
                dismiss();
            }
        });
        ButtonDrawableSetterCompat.setDrawableToButtonStart(
                mSetLockWallpaperButton,
                context.getDrawable(R.drawable.ic_lock_outline_24px));

        mSetBothWallpaperButton = layout.findViewById(R.id.set_both_wallpaper_button);
        mSetBothWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onSetBoth();
                dismiss();
            }
        });
        ButtonDrawableSetterCompat.setDrawableToButtonStart(
                mSetBothWallpaperButton,
                context.getDrawable(R.drawable.ic_smartphone_24px));

        updateButtonsVisibility();

        return dialog;
    }

    public void setHomeOptionAvailable(boolean homeAvailable) {
        mHomeAvailable = homeAvailable;
        updateButtonsVisibility();
    }

    private void updateButtonsVisibility() {
        if (mSetHomeWallpaperButton != null) {
            mSetHomeWallpaperButton.setVisibility(mHomeAvailable ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Interface which clients of this DialogFragment should implement in order to handle user actions
     * on the dialog's clickable elements.
     */
    public interface Listener {
        void onSetHomeScreen();

        void onSetLockScreen();

        void onSetBoth();
    }
}
