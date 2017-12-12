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
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;

import com.android.wallpaper.R;
import com.android.wallpaper.compat.ButtonDrawableSetterCompat;

/**
 * Dialog fragment which shows the "Set wallpaper" destination dialog for N+ devices. Lets user
 * choose whether to set the wallpaper on the home screen, lock screen, or both.
 */
public class SetWallpaperDialogFragment extends DialogFragment {

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

        Button setHomeWallpaperButton = (Button) layout.findViewById(R.id.set_home_wallpaper_button);
        setHomeWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onSetHomeScreen();
                dismiss();
            }
        });
        ButtonDrawableSetterCompat.setDrawableToButtonStart(
                setHomeWallpaperButton,
                context.getResources().getDrawable(R.drawable.material_ic_home_black_24));

        Button setLockWallpaperButton = (Button) layout.findViewById(R.id.set_lock_wallpaper_button);
        setLockWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onSetLockScreen();
                dismiss();
            }
        });
        ButtonDrawableSetterCompat.setDrawableToButtonStart(
                setLockWallpaperButton,
                context.getResources().getDrawable(R.drawable.material_ic_https_black_24));

        Button setBothWallpaperButton = (Button) layout.findViewById(R.id.set_both_wallpaper_button);
        setBothWallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onSetBoth();
                dismiss();
            }
        });
        ButtonDrawableSetterCompat.setDrawableToButtonStart(
                setBothWallpaperButton,
                context.getResources().getDrawable(R.drawable.material_ic_smartphone_black_24));

        return dialog;
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
