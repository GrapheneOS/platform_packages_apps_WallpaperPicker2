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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.DialogFragment;

import com.android.wallpaper.R;

/**
 * Dialog fragment which shows the error when an effect did not go as planned.
 */
public class EffectsErrorDialogFragment extends DialogFragment implements
        MyPhotosStarter.PermissionChangedListener {
    private int mTitleResId;
    private int mBodyResId;
    private AnimatedVectorDrawable mErrorAnimatedDrawable;

    public static final int SHOW_CATEGORY_REQUEST_CODE = 0;

    public EffectsErrorDialogFragment() {
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        Context context = getContext();

        @SuppressWarnings("RestrictTo")
        View layout =
                View.inflate(
                        new ContextThemeWrapper(getActivity(), R.style.LightDialogTheme),
                        R.layout.dialog_effect_error,
                        null);

        View customTitleView = View.inflate(context, R.layout.dialog_effect_error_title, null);
        TextView title = customTitleView.findViewById(R.id.dialog_effect_error_title);
        title.setText(mTitleResId);

        TextView body = layout.findViewById(R.id.dialog_effect_error_body);
        body.setText(mBodyResId);

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.LightDialogTheme)
                .setCustomTitle(customTitleView)
                .setView(layout)
                .create();

        Button buttonCancel = layout.findViewById(R.id.effect_error_cancel_button);
        buttonCancel.setOnClickListener((view) -> dismiss());

        Button buttonMyPhotos = layout.findViewById(R.id.effect_error_my_photos_button);
        buttonMyPhotos.setOnClickListener((view) -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            getActivity().startActivityForResult(intent, SHOW_CATEGORY_REQUEST_CODE);
            dismiss();
        });

        ImageView errorIllustration = layout.findViewById(R.id.error_bunny_illustration);
        Drawable drawable = errorIllustration.getDrawable();
        if (drawable instanceof AnimatedVectorDrawable) {
            mErrorAnimatedDrawable = (AnimatedVectorDrawable) drawable;
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        mErrorAnimatedDrawable.start();
    }

    public void setTitleResId(@StringRes int titleResId) {
        mTitleResId = titleResId;
    }

    public void setBodyResId(@StringRes int bodyResId) {
        mBodyResId = bodyResId;
    }

    @Override
    public void onPermissionsGranted() {
    }

    @Override
    public void onPermissionsDenied(boolean dontAskAgain) {
    }
}
