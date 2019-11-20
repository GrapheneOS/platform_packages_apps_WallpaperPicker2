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

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.wallpaper.R;
import com.android.wallpaper.compat.BuildCompat;
import com.android.wallpaper.model.LiveWallpaperInfo;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.ExploreIntentChecker;
import com.android.wallpaper.module.Injector;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.module.UserEventLogger;
import com.android.wallpaper.module.WallpaperPersister.Destination;
import com.android.wallpaper.module.WallpaperPreferences;
import com.android.wallpaper.module.WallpaperSetter;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetBehavior.State;

import java.util.Date;
import java.util.List;

/**
 * Base Fragment to display the UI for previewing an individual wallpaper
 */
public abstract class PreviewFragment extends Fragment implements
        SetWallpaperDialogFragment.Listener, SetWallpaperErrorDialogFragment.Listener,
        LoadWallpaperErrorDialogFragment.Listener {

    /**
     * User can view wallpaper and attributions in full screen, but "Set wallpaper" button is hidden.
     */
    static final int MODE_VIEW_ONLY = 0;

    /**
     * User can view wallpaper and attributions in full screen and click "Set wallpaper" to set the
     * wallpaper with pan and crop position to the device.
     */
    static final int MODE_CROP_AND_SET_WALLPAPER = 1;

    /**
     * Possible preview modes for the fragment.
     */
    @IntDef({
            MODE_VIEW_ONLY,
            MODE_CROP_AND_SET_WALLPAPER})
    public @interface PreviewMode {
    }

    public static final String ARG_WALLPAPER = "wallpaper";
    public static final String ARG_PREVIEW_MODE = "preview_mode";
    public static final String ARG_TESTING_MODE_ENABLED = "testing_mode_enabled";

    /**
     * Creates and returns new instance of {@link ImagePreviewFragment} with the provided wallpaper
     * set as an argument.
     */
    public static PreviewFragment newInstance(
            WallpaperInfo wallpaperInfo, @PreviewMode int mode, boolean testingModeEnabled) {

        boolean isLive = wallpaperInfo instanceof LiveWallpaperInfo;

        Bundle args = new Bundle();
        args.putParcelable(ARG_WALLPAPER, wallpaperInfo);
        args.putInt(ARG_PREVIEW_MODE, mode);
        args.putBoolean(ARG_TESTING_MODE_ENABLED, testingModeEnabled);

        PreviewFragment fragment = isLive ? new LivePreviewFragment() : new ImagePreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private static final String TAG_LOAD_WALLPAPER_ERROR_DIALOG_FRAGMENT =
            "load_wallpaper_error_dialog";
    private static final String TAG_SET_WALLPAPER_ERROR_DIALOG_FRAGMENT =
            "set_wallpaper_error_dialog";
    private static final int UNUSED_REQUEST_CODE = 1;
    private static final String TAG = "PreviewFragment";
    static final String KEY_BOTTOM_SHEET_STATE = "key_bottom_sheet_state";

    @PreviewMode
    protected int mPreviewMode;

    /**
     * When true, enables a test mode of operation -- in which certain UI features are disabled to
     * allow for UI tests to run correctly. Works around issue in ProgressDialog currently where the
     * dialog constantly keeps the UI thread alive and blocks a test forever.
     */
    protected boolean mTestingModeEnabled;

    protected WallpaperInfo mWallpaper;
    protected WallpaperSetter mWallpaperSetter;
    protected UserEventLogger mUserEventLogger;
    protected ViewGroup mBottomSheet;

    protected CheckBox mPreview;

    @SuppressWarnings("RestrictTo")
    @State
    protected int mBottomSheetInitialState;

    protected Intent mExploreIntent;

    /**
     * Staged error dialog fragments that were unable to be shown when the hosting activity didn't
     * allow committing fragment transactions.
     */
    private SetWallpaperErrorDialogFragment mStagedSetWallpaperErrorDialogFragment;
    private LoadWallpaperErrorDialogFragment mStagedLoadWallpaperErrorDialogFragment;

    protected static int getAttrColor(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        Context appContext = getContext().getApplicationContext();
        Injector injector = InjectorProvider.getInjector();

        mUserEventLogger = injector.getUserEventLogger(appContext);
        mWallpaper = getArguments().getParcelable(ARG_WALLPAPER);

        //noinspection ResourceType
        mPreviewMode = getArguments().getInt(ARG_PREVIEW_MODE);
        mTestingModeEnabled = getArguments().getBoolean(ARG_TESTING_MODE_ENABLED);
        mWallpaperSetter = new WallpaperSetter(injector.getWallpaperPersister(appContext),
                injector.getPreferences(appContext), mUserEventLogger, mTestingModeEnabled);

        setHasOptionsMenu(true);

        // Allow the layout to draw fullscreen even behind the status bar, so we can set as the status
        // bar color a color that has a custom translucency in the theme.
        Window window = activity.getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        List<String> attributions = mWallpaper.getAttributions(activity);
        if (attributions.size() > 0 && attributions.get(0) != null) {
            activity.setTitle(attributions.get(0));
        }
    }

    @LayoutRes
    protected abstract int getLayoutResId();

    @Override
    @CallSuper
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutResId(), container, false);

        // Set toolbar as the action bar.
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Use updated fancy arrow icon for O+.
        if (BuildCompat.isAtLeastO()) {
            Drawable navigationIcon = getResources().getDrawable(
                    R.drawable.material_ic_arrow_back_black_24);

            // This Drawable's state is shared across the app, so make a copy of it before applying a
            // color tint as not to affect other clients elsewhere in the app.
            navigationIcon = navigationIcon.getConstantState().newDrawable().mutate();
            navigationIcon.setColorFilter(
                    getResources().getColor(R.color.material_white_100), Mode.SRC_IN);
            navigationIcon.setAutoMirrored(true);
            toolbar.setNavigationIcon(navigationIcon);
        }

        ViewCompat.setPaddingRelative(toolbar,
        /* start */ getResources().getDimensionPixelSize(
                        R.dimen.preview_toolbar_up_button_start_padding),
        /* top */ 0,
        /* end */ getResources().getDimensionPixelSize(
                        R.dimen.preview_toolbar_set_wallpaper_button_end_padding),
        /* bottom */ 0);

        mBottomSheet = view.findViewById(getBottomSheetResId());
        setUpBottomSheetView(mBottomSheet);

        // Workaround as we don't have access to bottomDialogCornerRadius, mBottomSheet radii are
        // set to dialogCornerRadius by default.
        GradientDrawable bottomSheetBackground = (GradientDrawable) mBottomSheet.getBackground();
        float[] radii = bottomSheetBackground.getCornerRadii();
        for (int i = 0; i < radii.length; i++) {
            radii[i]*=2f;
        }
        bottomSheetBackground = ((GradientDrawable)bottomSheetBackground.mutate());
        bottomSheetBackground.setCornerRadii(radii);
        mBottomSheet.setBackground(bottomSheetBackground);

        mBottomSheetInitialState = (savedInstanceState == null) ? STATE_EXPANDED
                : savedInstanceState.getInt(KEY_BOTTOM_SHEET_STATE, STATE_EXPANDED);
        setUpBottomSheetListeners();

        return view;
    }

    protected abstract void setUpBottomSheetView(ViewGroup bottomSheet);

    @IdRes
    protected abstract int getBottomSheetResId();

    protected int getDeviceDefaultTheme() {
        return android.R.style.Theme_DeviceDefault;
    }

    @Override
    public void onResume() {
        super.onResume();

        WallpaperPreferences preferences =
                InjectorProvider.getInjector().getPreferences(getActivity());
        preferences.setLastAppActiveTimestamp(new Date().getTime());

        // Show the staged 'load wallpaper' or 'set wallpaper' error dialog fragments if there is
        // one that was unable to be shown earlier when this fragment's hosting activity didn't
        // allow committing fragment transactions.
        if (mStagedLoadWallpaperErrorDialogFragment != null) {
            mStagedLoadWallpaperErrorDialogFragment.show(
                    requireFragmentManager(), TAG_LOAD_WALLPAPER_ERROR_DIALOG_FRAGMENT);
            mStagedLoadWallpaperErrorDialogFragment = null;
        }
        if (mStagedSetWallpaperErrorDialogFragment != null) {
            mStagedSetWallpaperErrorDialogFragment.show(
                    requireFragmentManager(), TAG_SET_WALLPAPER_ERROR_DIALOG_FRAGMENT);
            mStagedSetWallpaperErrorDialogFragment = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.preview_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        setupPreviewMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // The Preview screen has multiple entry points. It could be opened from either
            // the IndividualPreviewActivity, the "My photos" selection (by way of
            // TopLevelPickerActivity), or from a system "crop and set wallpaper" intent.
            // Therefore, handle the Up button as a global Back.
            requireActivity().onBackPressed();
            return true;
        }

        return false;
    }

    private void setupPreviewMenu(Menu menu) {
        mPreview = (CheckBox) menu.findItem(R.id.preview).getActionView();
        mPreview.setChecked(mBottomSheetInitialState == STATE_COLLAPSED);
        mPreview.setOnClickListener(this::setPreviewBehavior);
    }

    protected void setPreviewChecked(boolean checked) {
        if (mPreview != null) {
            mPreview.setChecked(checked);
            int resId = checked ? R.string.expand_attribution_panel
                    : R.string.collapse_attribution_panel;
            mPreview.setContentDescription(getResources().getString(resId));
        }
    }

    private void setPreviewBehavior(View v) {
        CheckBox checkbox = (CheckBox) v;
        BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(mBottomSheet);

        if (checkbox.isChecked()) {
            behavior.setState(STATE_COLLAPSED);
        } else {
            behavior.setState(STATE_EXPANDED);
        }
    }

    protected void setUpSetWallpaperButton(Button setWallpaperButton) {
        if (mPreviewMode == MODE_VIEW_ONLY) {
            setWallpaperButton.setVisibility(View.GONE);
        } else {
            setWallpaperButton.setVisibility(View.VISIBLE);
            setWallpaperButton.setOnClickListener(this::onSetWallpaperClicked);
        }
    }

    protected void setUpExploreButton(Button exploreButton) {
        exploreButton.setVisibility(View.GONE);
        if (mExploreIntent == null) {
            return;
        }
        Context context = requireContext();
        exploreButton.setVisibility(View.VISIBLE);
        exploreButton.setText(context.getString(
                mWallpaper.getActionLabelRes(context)));

        exploreButton.setOnClickListener(view -> {
            mUserEventLogger.logActionClicked(mWallpaper.getCollectionId(context),
                    mWallpaper.getActionLabelRes(context));

            startActivity(mExploreIntent);
        });
    }

    protected void setUpExploreIntent(@Nullable Runnable callback) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        String actionUrl = mWallpaper.getActionUrl(context);
        if (actionUrl != null && !actionUrl.isEmpty()) {
            Uri exploreUri = Uri.parse(mWallpaper.getActionUrl(context));
            ExploreIntentChecker intentChecker =
                    InjectorProvider.getInjector().getExploreIntentChecker(context);

            intentChecker.fetchValidActionViewIntent(exploreUri, exploreIntent -> {
                if (getActivity() == null) {
                    return;
                }

                mExploreIntent = exploreIntent;
                if (callback != null) {
                    callback.run();
                }
            });
        } else {
            if (callback != null) {
                callback.run();
            }
        }
    }

    @Override
    public void onSet(int destination) {
        setCurrentWallpaper(destination);
    }

    @Override
    public void onClickTryAgain(@Destination int wallpaperDestination) {
        setCurrentWallpaper(wallpaperDestination);
    }

    @Override
    public void onClickOk() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWallpaperSetter.cleanUp();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
        outState.putInt(KEY_BOTTOM_SHEET_STATE, bottomSheetBehavior.getState());
    }

    private void onSetWallpaperClicked(View button) {
        mWallpaperSetter.requestDestination(getContext(), getFragmentManager(), this,
                mWallpaper instanceof LiveWallpaperInfo);
    }

    private void setUpBottomSheetListeners() {
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                // Don't respond to lingering state change events occurring after the fragment has
                // already been detached from the activity. Else, IllegalStateException may occur
                // when trying to fetch resources.
                if (getActivity() == null) {
                    return;
                }
                switch (newState) {
                    case STATE_COLLAPSED:
                        setPreviewChecked(true /* checked */);
                        break;
                    case STATE_EXPANDED:
                        setPreviewChecked(false /* checked */);
                        break;
                    default:
                        Log.v(TAG, "Ignoring BottomSheet state: " + newState);
                }
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                float alpha;
                if (slideOffset >= 0) {
                    alpha = slideOffset;
                } else {
                    alpha = 1f - slideOffset;
                }
                setBottomSheetContentAlpha(alpha);
            }
        });
    }

    protected void setBottomSheetContentAlpha(float alpha) {

    }

    /**
     * Sets current wallpaper to the device based on current zoom and scroll state.
     *
     * @param destination The wallpaper destination i.e. home vs. lockscreen vs. both.
     */
    protected abstract void setCurrentWallpaper(@Destination int destination);

    protected void finishActivityWithResultOk() {
        Activity activity = requireActivity();
        try {
            Toast.makeText(activity,
                    R.string.wallpaper_set_successfully_message, Toast.LENGTH_SHORT).show();
        } catch (NotFoundException e) {
            Log.e(TAG, "Could not show toast " + e);
        }
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        activity.setResult(Activity.RESULT_OK);
        activity.finish();
    }

    protected void showSetWallpaperErrorDialog(@Destination int wallpaperDestination) {
        SetWallpaperErrorDialogFragment newFragment = SetWallpaperErrorDialogFragment.newInstance(
                R.string.set_wallpaper_error_message, wallpaperDestination);
        newFragment.setTargetFragment(this, UNUSED_REQUEST_CODE);

        // Show 'set wallpaper' error dialog now if it's safe to commit fragment transactions,
        // otherwise stage it for later when the hosting activity is in a state to commit fragment
        // transactions.
        BasePreviewActivity activity = (BasePreviewActivity) requireActivity();
        if (activity.isSafeToCommitFragmentTransaction()) {
            newFragment.show(requireFragmentManager(), TAG_SET_WALLPAPER_ERROR_DIALOG_FRAGMENT);
        } else {
            mStagedSetWallpaperErrorDialogFragment = newFragment;
        }
    }

    /**
     * Shows 'load wallpaper' error dialog now or stage it to be shown when the hosting activity is
     * in a state that allows committing fragment transactions.
     */
    protected void showLoadWallpaperErrorDialog() {
        LoadWallpaperErrorDialogFragment dialogFragment =
                LoadWallpaperErrorDialogFragment.newInstance();
        dialogFragment.setTargetFragment(this, UNUSED_REQUEST_CODE);

        // Show 'load wallpaper' error dialog now or stage it to be shown when the hosting
        // activity is in a state that allows committing fragment transactions.
        BasePreviewActivity activity = (BasePreviewActivity) getActivity();
        if (activity != null && activity.isSafeToCommitFragmentTransaction()) {
            dialogFragment.show(requireFragmentManager(), TAG_LOAD_WALLPAPER_ERROR_DIALOG_FRAGMENT);
        } else {
            mStagedLoadWallpaperErrorDialogFragment = dialogFragment;
        }
    }

    @IntDef({
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE})
    private @interface ActivityInfoScreenOrientation {
    }

    /**
     * Returns whether layout direction is RTL (or false for LTR). Since native RTL layout support
     * was added in API 17, returns false for versions lower than 17.
     */
    protected boolean isRtl() {
        return getResources().getConfiguration().getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL;
    }
}