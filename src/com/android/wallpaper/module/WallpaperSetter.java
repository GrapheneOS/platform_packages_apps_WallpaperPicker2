package com.android.wallpaper.module;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import com.android.wallpaper.R;
import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.model.LiveWallpaperInfo;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.UserEventLogger.WallpaperSetFailureReason;
import com.android.wallpaper.module.WallpaperPersister.Destination;
import com.android.wallpaper.module.WallpaperPersister.SetWallpaperCallback;
import com.android.wallpaper.picker.SetWallpaperDialogFragment;
import com.android.wallpaper.picker.SetWallpaperDialogFragment.Listener;
import com.android.wallpaper.util.ThrowableAnalyzer;

import com.bumptech.glide.Glide;

/**
 * Helper class used to set the current wallpaper. It handles showing the destination request dialog
 * and actually setting the wallpaper on a given destination.
 * It is expected to be instantiated within a Fragment or Activity, and {@link #cleanUp()} should
 * be called from its owner's onDestroy method (or equivalent).
 */
public class WallpaperSetter {

    private static final String PROGRESS_DIALOG_NO_TITLE = null;
    private static final boolean PROGRESS_DIALOG_INDETERMINATE = true;

    private static final int UNUSED_REQUEST_CODE = 1;
    private static final String TAG_SET_WALLPAPER_DIALOG_FRAGMENT = "set_wallpaper_dialog";

    private final WallpaperPersister mWallpaperPersister;
    private final WallpaperPreferences mPreferences;
    private final boolean mTestingModeEnabled;
    private final UserEventLogger mUserEventLogger;
    private ProgressDialog mProgressDialog;
    private int mCurrentScreenOrientation;

    public WallpaperSetter(WallpaperPersister wallpaperPersister,
            WallpaperPreferences preferences, UserEventLogger userEventLogger,
            boolean isTestingModeEnabled) {
        mTestingModeEnabled = isTestingModeEnabled;
        mWallpaperPersister = wallpaperPersister;
        mPreferences = preferences;
        mUserEventLogger = userEventLogger;
    }

    /**
     * Sets current wallpaper to the device based on current zoom and scroll state.
     *
     * @param containerActivity main Activity that owns the current fragment
     * @param wallpaper info for the actual wallpaper to set
     * @param wallpaperAsset  Wallpaper asset from which to retrieve image data.
     * @param destination The wallpaper destination i.e. home vs. lockscreen vs. both.
     * @param wallpaperScale Scaling factor applied to the source image before setting the
     *                       wallpaper to the device.
     * @param cropRect Desired crop area of the wallpaper in post-scale units. If null, then the
     *                 wallpaper image will be set without any scaling or cropping.
     * @param callback optional callback to be notified when the wallpaper is set.
     */
    public void setCurrentWallpaper(Activity containerActivity, WallpaperInfo wallpaper,
            Asset wallpaperAsset, @Destination final int destination, float wallpaperScale,
            @Nullable Rect cropRect, @Nullable SetWallpaperCallback callback) {
        mPreferences.setPendingWallpaperSetStatus(
                WallpaperPreferences.WALLPAPER_SET_PENDING);

        // Save current screen rotation so we can temporarily disable rotation while setting the
        // wallpaper and restore after setting the wallpaper finishes.
        saveAndLockScreenOrientation(containerActivity);

        // Clear MosaicView tiles and Glide's cache and pools to reclaim memory for final cropped
        // bitmap.
        Glide.get(containerActivity).clearMemory();

        // ProgressDialog endlessly updates the UI thread, keeping it from going idle which therefore
        // causes Espresso to hang once the dialog is shown.
        if (!mTestingModeEnabled && !containerActivity.isFinishing()) {
            int themeResId = (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP)
                    ? R.style.ProgressDialogThemePreL : R.style.LightDialogTheme;
            mProgressDialog = new ProgressDialog(containerActivity, themeResId);

            mProgressDialog.setTitle(PROGRESS_DIALOG_NO_TITLE);
            mProgressDialog.setMessage(containerActivity.getString(
                            R.string.set_wallpaper_progress_message));
            mProgressDialog.setIndeterminate(PROGRESS_DIALOG_INDETERMINATE);
            mProgressDialog.show();
        }

        mWallpaperPersister.setIndividualWallpaper(
                wallpaper, wallpaperAsset, cropRect,
                wallpaperScale, destination, new SetWallpaperCallback() {
                    @Override
                    public void onSuccess() {
                        onWallpaperApplied(wallpaper, containerActivity);
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        onWallpaperApplyError(throwable, containerActivity);
                        if (callback != null) {
                            callback.onError(throwable);
                        }
                    }
                });
    }

    private void onWallpaperApplied(WallpaperInfo wallpaper, Activity containerActivity) {
        mUserEventLogger.logWallpaperSet(
                wallpaper.getCollectionId(containerActivity),
                wallpaper.getWallpaperId());
        mPreferences.setPendingWallpaperSetStatus(
                WallpaperPreferences.WALLPAPER_SET_NOT_PENDING);
        mUserEventLogger.logWallpaperSetResult(
                UserEventLogger.WALLPAPER_SET_RESULT_SUCCESS);

        cleanUp();
        restoreScreenOrientation(containerActivity);
    }

    private void onWallpaperApplyError(Throwable throwable, Activity containerActivity) {
        mPreferences.setPendingWallpaperSetStatus(
                WallpaperPreferences.WALLPAPER_SET_NOT_PENDING);
        mUserEventLogger.logWallpaperSetResult(
                UserEventLogger.WALLPAPER_SET_RESULT_FAILURE);
        @WallpaperSetFailureReason int failureReason = ThrowableAnalyzer.isOOM(
                throwable)
                ? UserEventLogger.WALLPAPER_SET_FAILURE_REASON_OOM
                : UserEventLogger.WALLPAPER_SET_FAILURE_REASON_OTHER;
        mUserEventLogger.logWallpaperSetFailureReason(failureReason);

        cleanUp();
        restoreScreenOrientation(containerActivity);
    }

    /**
     * Call this method to clean up this instance's state.
     */
    public void cleanUp() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**
     * Show a dialog asking the user for the Wallpaper's destination
     * (eg, "Home screen", "Lock Screen")
     * @param listener {@link SetWallpaperDialogFragment.Listener} that will receive the response.
     * @see Destination
     */
    public void requestDestination(Context context, FragmentManager fragmentManager,
            Listener listener) {
        requestDestination(context, fragmentManager, R.string.set_wallpaper_dialog_message,
                listener);
    }

    /**
     * Show a dialog asking the user for the Wallpaper's destination
     * (eg, "Home screen", "Lock Screen")
     * @param listener {@link SetWallpaperDialogFragment.Listener} that will receive the response.
     * @param titleResId title for the dialog
     * @see Destination
     */
    public void requestDestination(Context context, FragmentManager fragmentManager,
            @StringRes int titleResId, Listener listener) {
        CurrentWallpaperInfoFactory factory = InjectorProvider.getInjector()
                .getCurrentWallpaperFactory(context);

        factory.createCurrentWallpaperInfos((homeWallpaper, lockWallpaper, presentationMode) -> {
            SetWallpaperDialogFragment setWallpaperDialog = new SetWallpaperDialogFragment();
            setWallpaperDialog.setTitleResId(titleResId);
            setWallpaperDialog.setListener(listener);
            if (homeWallpaper instanceof LiveWallpaperInfo && lockWallpaper == null) {
                // if the lock wallpaper is a live wallpaper, we cannot set a home-only static one
                setWallpaperDialog.setHomeOptionAvailable(false);
            }
            setWallpaperDialog.show(fragmentManager, TAG_SET_WALLPAPER_DIALOG_FRAGMENT);
        }, true); // Force refresh as the wallpaper may have been set while this fragment was paused
    }

    private void saveAndLockScreenOrientation(Activity activity) {
        mCurrentScreenOrientation = activity.getRequestedOrientation();
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    private void restoreScreenOrientation(Activity activity) {
        if (activity.getRequestedOrientation() != mCurrentScreenOrientation) {
            activity.setRequestedOrientation(mCurrentScreenOrientation);
        }
    }
}