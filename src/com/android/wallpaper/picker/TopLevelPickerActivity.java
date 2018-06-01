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

import android.Manifest.permission;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.service.wallpaper.WallpaperService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.OnTabSelectedListener;
import android.support.design.widget.TabLayout.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.wallpaper.R;
import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.compat.ButtonDrawableSetterCompat;
import com.android.wallpaper.compat.WallpaperManagerCompat;
import com.android.wallpaper.config.Flags;
import com.android.wallpaper.model.Category;
import com.android.wallpaper.model.CategoryProvider;
import com.android.wallpaper.model.CategoryReceiver;
import com.android.wallpaper.model.ImageWallpaperInfo;
import com.android.wallpaper.model.InlinePreviewIntentFactory;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.CurrentWallpaperInfoFactory;
import com.android.wallpaper.module.CurrentWallpaperInfoFactory.WallpaperInfoCallback;
import com.android.wallpaper.module.DailyLoggingAlarmScheduler;
import com.android.wallpaper.module.ExploreIntentChecker;
import com.android.wallpaper.module.FormFactorChecker;
import com.android.wallpaper.module.FormFactorChecker.FormFactor;
import com.android.wallpaper.module.Injector;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.module.NetworkStatusNotifier;
import com.android.wallpaper.module.NetworkStatusNotifier.NetworkStatus;
import com.android.wallpaper.module.PackageStatusNotifier;
import com.android.wallpaper.module.UserEventLogger;
import com.android.wallpaper.module.UserEventLogger.WallpaperSetFailureReason;
import com.android.wallpaper.module.WallpaperPersister;
import com.android.wallpaper.module.WallpaperPersister.Destination;
import com.android.wallpaper.module.WallpaperPersister.SetWallpaperCallback;
import com.android.wallpaper.module.WallpaperPersister.WallpaperPosition;
import com.android.wallpaper.module.WallpaperPreferences;
import com.android.wallpaper.module.WallpaperPreferences.PresentationMode;
import com.android.wallpaper.module.WallpaperRotationRefresher;
import com.android.wallpaper.module.WallpaperRotationRefresher.Listener;
import com.android.wallpaper.picker.PreviewActivity.PreviewActivityIntentFactory;
import com.android.wallpaper.picker.ViewOnlyPreviewActivity.ViewOnlyPreviewActivityIntentFactory;
import com.android.wallpaper.picker.WallpaperDisabledFragment.WallpaperSupportLevel;
import com.android.wallpaper.picker.individual.IndividualPickerActivity.IndividualPickerActivityIntentFactory;
import com.android.wallpaper.picker.individual.IndividualPickerFragment;
import com.android.wallpaper.util.ScreenSizeCalculator;
import com.android.wallpaper.util.ThrowableAnalyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity allowing users to select a category of wallpapers to choose from.
 */
public class TopLevelPickerActivity extends BaseActivity implements WallpapersUiContainer,
        CurrentWallpaperBottomSheetPresenter, SetWallpaperErrorDialogFragment.Listener,
        MyPhotosLauncher {
    private static final int SHOW_CATEGORY_REQUEST_CODE = 0;
    private static final int PREVIEW_WALLPAPER_REQUEST_CODE = 1;
    private static final int VIEW_ONLY_PREVIEW_WALLPAPER_REQUEST_CODE = 2;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 3;

    private static final String TAG_SET_WALLPAPER_ERROR_DIALOG_FRAGMENT =
            "toplevel_set_wallpaper_error_dialog";

    private static final String TAG = "TopLevelPicker";
    private static final String KEY_SELECTED_CATEGORY_TAB = "selected_category_tab";

    private IndividualPickerActivityIntentFactory mPickerIntentFactory;
    private InlinePreviewIntentFactory mPreviewIntentFactory;
    private InlinePreviewIntentFactory mViewOnlyPreviewIntentFactory;
    private int mLastSelectedCategoryTabIndex;
    @FormFactor
    private int mFormFactor;
    private WallpaperPreferences mPreferences;
    private UserEventLogger mUserEventLogger;
    private NetworkStatusNotifier mNetworkStatusNotifier;
    private NetworkStatusNotifier.Listener mNetworkStatusListener;
    private PackageStatusNotifier mPackageStatusNotifier;
    private WallpaperPersister mWallpaperPersister;
    private boolean mWasCustomPhotoWallpaperSet;
    @WallpaperPosition
    private int mCustomPhotoWallpaperPosition;

    /**
     * Progress dialogs for "refresh daily wallpaper" and "set wallpaper" operations.
     */
    private ProgressDialog mRefreshWallpaperProgressDialog;
    private ProgressDialog mSetWallpaperProgressDialog;

    /**
     * Designates a test mode of operation -- in which certain UI features are disabled to allow for
     * UI tests to run correctly.
     */
    private boolean mTestingMode;

    /**
     * UI for the "currently set wallpaper" BottomSheet.
     */
    private LinearLayout mBottomSheet;
    private ImageView mCurrentWallpaperImage;
    private TextView mCurrentWallpaperPresentationMode;
    private TextView mCurrentWallpaperTitle;
    private TextView mCurrentWallpaperSubtitle;
    private Button mCurrentWallpaperExploreButton;
    private Button mCurrentWallpaperSkipWallpaperButton;
    private FrameLayout mFragmentContainer;
    private FrameLayout mLoadingIndicatorContainer;
    private LinearLayout mWallpaperPositionOptions;

    private List<PermissionChangedListener> mPermissionChangedListeners;

    /**
     * Staged error dialog fragments that were unable to be shown when the activity didn't allow
     * committing fragment transactions.
     */
    private SetWallpaperErrorDialogFragment mStagedSetWallpaperErrorDialogFragment;

    /**
     * A wallpaper pending set to the device--we retain a reference to this in order to facilitate
     * retry or re-crop operations.
     */
    private WallpaperInfo mPendingSetWallpaperInfo;
    private PackageStatusNotifier.Listener mLiveWallpaperStatusListener;
    private PackageStatusNotifier.Listener mThirdPartyStatusListener;
    private CategoryProvider mCategoryProvider;

    private static int getTextColorIdForWallpaperPositionButton(boolean isSelected) {
        return isSelected ? R.color.accent_color : R.color.material_grey500;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPickerIntentFactory = new IndividualPickerActivityIntentFactory();
        mPreviewIntentFactory = new PreviewActivityIntentFactory();
        mViewOnlyPreviewIntentFactory = new ViewOnlyPreviewActivityIntentFactory();
        mLastSelectedCategoryTabIndex = -1;

        Injector injector = InjectorProvider.getInjector();
        mCategoryProvider = injector.getCategoryProvider(this);
        mPreferences = injector.getPreferences(this);
        mUserEventLogger = injector.getUserEventLogger(this);
        mNetworkStatusNotifier = injector.getNetworkStatusNotifier(this);
        mPackageStatusNotifier = injector.getPackageStatusNotifier(this);
        final FormFactorChecker formFactorChecker = injector.getFormFactorChecker(this);
        mFormFactor = formFactorChecker.getFormFactor();
        mWallpaperPersister = injector.getWallpaperPersister(this);
        mWasCustomPhotoWallpaperSet = false;

        mPermissionChangedListeners = new ArrayList<>();

        @WallpaperSupportLevel int wallpaperSupportLevel = getWallpaperSupportLevel();
        if (wallpaperSupportLevel != WallpaperDisabledFragment.SUPPORTED_CAN_SET) {
            setContentView(R.layout.activity_single_fragment);

            FragmentManager fm = getSupportFragmentManager();
            WallpaperDisabledFragment wallpaperDisabledFragment =
                    WallpaperDisabledFragment.newInstance(wallpaperSupportLevel);
            fm.beginTransaction()
                    .add(R.id.fragment_container, wallpaperDisabledFragment)
                    .commit();
            return;
        }

        if (mFormFactor == FormFactorChecker.FORM_FACTOR_MOBILE) {
            initializeMobile();
        } else { // DESKTOP
            initializeDesktop(savedInstanceState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Show the staged 'load wallpaper' or 'set wallpaper' error dialog fragments if there is one
        // that was unable to be shown earlier when this fragment's hosting activity didn't allow
        // committing fragment transactions.
        if (mStagedSetWallpaperErrorDialogFragment != null) {
            mStagedSetWallpaperErrorDialogFragment.show(
                    getSupportFragmentManager(), TAG_SET_WALLPAPER_ERROR_DIALOG_FRAGMENT);
            mStagedSetWallpaperErrorDialogFragment = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mNetworkStatusListener != null) {
            mNetworkStatusNotifier.unregisterListener(mNetworkStatusListener);
        }

        if (mPackageStatusNotifier != null) {
            mPackageStatusNotifier.removeListener(mLiveWallpaperStatusListener);
            mPackageStatusNotifier.removeListener(mThirdPartyStatusListener);
        }

        if (mRefreshWallpaperProgressDialog != null) {
            mRefreshWallpaperProgressDialog.dismiss();
        }
        if (mSetWallpaperProgressDialog != null) {
            mSetWallpaperProgressDialog.dismiss();
        }

        if (mFormFactor == FormFactorChecker.FORM_FACTOR_DESKTOP && mWasCustomPhotoWallpaperSet) {
            mUserEventLogger.logWallpaperPosition(mCustomPhotoWallpaperPosition);
        }
    }

    @Override
    public void requestCustomPhotoPicker(PermissionChangedListener listener) {
        if (!isReadExternalStoragePermissionGranted()) {
            PermissionChangedListener wrappedListener = new PermissionChangedListener() {
                @Override
                public void onPermissionsGranted() {
                    listener.onPermissionsGranted();
                    showCustomPhotoPicker();
                }

                @Override
                public void onPermissionsDenied(boolean dontAskAgain) {
                    listener.onPermissionsDenied(dontAskAgain);
                }
            };
            requestExternalStoragePermission(wrappedListener);

            return;
        }

        showCustomPhotoPicker();
    }

    void requestExternalStoragePermission(PermissionChangedListener listener) {
        mPermissionChangedListeners.add(listener);
        requestPermissions(
                new String[]{permission.READ_EXTERNAL_STORAGE},
                READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
    }

    /**
     * Returns whether READ_EXTERNAL_STORAGE has been granted for the application.
     */
    boolean isReadExternalStoragePermissionGranted() {
        return getPackageManager().checkPermission(permission.READ_EXTERNAL_STORAGE,
                getPackageName()) == PackageManager.PERMISSION_GRANTED;
    }

    private void showCustomPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, SHOW_CATEGORY_REQUEST_CODE);
    }

    private void initializeMobile() {
        setContentView(R.layout.activity_single_fragment_with_toolbar);

        // Set toolbar as the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        boolean forceCategoryRefresh = false;
        if (fragment == null) {
            // App launch specific logic: log the "app launched" event and set up daily logging.
            mUserEventLogger.logAppLaunched();
            DailyLoggingAlarmScheduler.setAlarm(getApplicationContext());

            CategoryPickerFragment newFragment = new CategoryPickerFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container, newFragment)
                    .commit();

            forceCategoryRefresh = true;
        }

        populateCategories(-1, forceCategoryRefresh);
        mLiveWallpaperStatusListener = this::updateLiveWallpapersCategories;
        mThirdPartyStatusListener = this::updateThirdPartyCategories;
        mPackageStatusNotifier.addListener(mLiveWallpaperStatusListener,
                WallpaperService.SERVICE_INTERFACE);
        mPackageStatusNotifier.addListener(mThirdPartyStatusListener,
                Intent.ACTION_SET_WALLPAPER);
    }

    private void updateThirdPartyCategories(String packageName, @PackageStatusNotifier.PackageStatus
            int status) {

        if (status == PackageStatusNotifier.PackageStatus.ADDED) {
            mCategoryProvider.fetchCategories(new CategoryReceiver() {
                @Override
                public void onCategoryReceived(Category category) {
                    if (category.supportsThirdParty() && category.containsThirdParty(packageName)) {
                        addCategory(category, false);
                    }
                }

                @Override
                public void doneFetchingCategories() {
                    // Do nothing here.
                }
            }, true);
        } else if (status == PackageStatusNotifier.PackageStatus.REMOVED) {
            Category oldCategory = findThirdPartyCategory(packageName);
            if (oldCategory != null) {
                mCategoryProvider.fetchCategories(new CategoryReceiver() {
                    @Override
                    public void onCategoryReceived(Category category) {
                       // Do nothing here
                    }

                    @Override
                    public void doneFetchingCategories() {
                        removeCategory(oldCategory);
                    }
                }, true);
            }
        } else {
            // CHANGED package, let's reload all categories as we could have more or fewer now
            populateCategories(-1, true);
        }
    }

    private Category findThirdPartyCategory(String packageName) {
        int size = mCategoryProvider.getSize();
        for (int i = 0; i < size; i++) {
            Category category = mCategoryProvider.getCategory(i);
            if (category.supportsThirdParty() && category.containsThirdParty(packageName)) {
                return category;
            }
        }
        return null;
    }

    private void updateLiveWallpapersCategories(String packageName,
                                                @PackageStatusNotifier.PackageStatus int status) {
        String liveWallpaperCollectionId = getString(R.string.live_wallpaper_collection_id);
        Category oldLiveWallpapersCategory = mCategoryProvider.getCategory(
                liveWallpaperCollectionId);
        if (status == PackageStatusNotifier.PackageStatus.REMOVED
                && (oldLiveWallpapersCategory == null
                    || !oldLiveWallpapersCategory.containsThirdParty(packageName))) {
            // If we're removing a wallpaper and the live category didn't contain it already,
            // there's nothing to do.
            return;
        }
        mCategoryProvider.fetchCategories(new CategoryReceiver() {
            @Override
            public void onCategoryReceived(Category category) {
                // Do nothing here
            }

            @Override
            public void doneFetchingCategories() {
                Category liveWallpapersCategory =
                        mCategoryProvider.getCategory(liveWallpaperCollectionId);
                if (liveWallpapersCategory == null) {
                    // There are no more 3rd party live wallpapers, so the Category is gone.
                    removeCategory(oldLiveWallpapersCategory);
                } else {
                    if (oldLiveWallpapersCategory != null) {
                        updateCategory(liveWallpapersCategory);
                    } else {
                        addCategory(liveWallpapersCategory, false);
                    }
                }
            }
        }, true);
    }

    private void initializeDesktop(Bundle savedInstanceState) {
        setContentView(R.layout.activity_top_level_desktop);

        mBottomSheet = (LinearLayout) findViewById(R.id.bottom_sheet);
        mCurrentWallpaperImage = (ImageView) mBottomSheet.findViewById(R.id.current_wallpaper_image);
        mCurrentWallpaperImage.getLayoutParams().width = getSingleWallpaperImageWidthPx();

        mCurrentWallpaperPresentationMode =
                (TextView) mBottomSheet.findViewById(R.id.current_wallpaper_presentation_mode);
        mCurrentWallpaperTitle = (TextView) findViewById(R.id.current_wallpaper_title);
        mCurrentWallpaperSubtitle = (TextView) findViewById(R.id.current_wallpaper_subtitle);
        mCurrentWallpaperExploreButton = (Button) findViewById(
                R.id.current_wallpaper_explore_button);
        mCurrentWallpaperSkipWallpaperButton = (Button) findViewById(
                R.id.current_wallpaper_skip_wallpaper_button);
        mFragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);
        mLoadingIndicatorContainer = (FrameLayout) findViewById(R.id.loading_indicator_container);
        mWallpaperPositionOptions = (LinearLayout) findViewById(
                R.id.desktop_wallpaper_position_options);

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new OnTabSelectedListener() {
            @Override
            public void onTabSelected(Tab tab) {
                Category category = (Category) tab.getTag();
                show(category.getCollectionId());
                mLastSelectedCategoryTabIndex = tabLayout.getSelectedTabPosition();
            }

            @Override
            public void onTabUnselected(Tab tab) {
            }

            @Override
            public void onTabReselected(Tab tab) {
                Category category = (Category) tab.getTag();
                // If offline, "My photos" may be the only visible category. In this case we want to allow
                // re-selection so user can still select a photo as wallpaper while offline.
                if (!category.isEnumerable()) {
                    onTabSelected(tab);
                }
            }
        });

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            // App launch specific logic: log the "app launched" event and set up daily logging.
            mUserEventLogger.logAppLaunched();
            DailyLoggingAlarmScheduler.setAlarm(getApplicationContext());
        }

        mNetworkStatusListener = new NetworkStatusNotifier.Listener() {
            @Override
            public void onNetworkChanged(@NetworkStatus int networkStatus) {
                initializeDesktopBasedOnNetwork(networkStatus, savedInstanceState);
            }
        };
        // Upon registering a listener, the onNetworkChanged method is immediately called with the
        // initial network status.
        mNetworkStatusNotifier.registerListener(mNetworkStatusListener);
    }

    private void initializeDesktopBasedOnNetwork(@NetworkStatus int networkStatus,
                                                 Bundle savedInstanceState) {
        if (networkStatus == NetworkStatusNotifier.NETWORK_CONNECTED) {
            initializeDesktopOnline(savedInstanceState);
        } else {
            initializeDesktopOffline();
        }
    }

    private void initializeDesktopOnline(Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        // Require a category refresh if this is the first load of the app or if the app is now
        // returning online after having been offline.
        boolean forceCategoryRefresh = fragment == null || fragment instanceof OfflineDesktopFragment;

        if (fragment != null) {
            fm.beginTransaction()
                    .remove(fragment)
                    .commit();
        }

        int selectedTabPosition = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_SELECTED_CATEGORY_TAB) : -1;
        populateCategories(selectedTabPosition, forceCategoryRefresh);

        setDesktopLoading(true);
        setUpBottomSheet();
        refreshCurrentWallpapers(null /* refreshListener */);
    }

    private void initializeDesktopOffline() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment != null) {
            fm.beginTransaction()
                    .remove(fragment)
                    .commit();
        }
        OfflineDesktopFragment newFragment = new OfflineDesktopFragment();
        fm.beginTransaction()
                .add(R.id.fragment_container, newFragment)
                .commit();

        // Reset the last selected category tab index to ensure the app doesn't try to reselect a tab
        // for a category not yet repopulated.
        mLastSelectedCategoryTabIndex = -1;

        populateCategories(-1 /* selectedTabPosition */, true /* forceCategoryRefresh */);

        setDesktopLoading(false);
        setCurrentWallpapersExpanded(false);
    }

    /**
     * Sets the status of the loading indicator overlay in desktop mode.
     *
     * @param loading Whether an indeterminate loading indicator is displayed in place of the main
     *                fragment.
     */
    private void setDesktopLoading(boolean loading) {
        if (loading) {
            mLoadingIndicatorContainer.setVisibility(View.VISIBLE);
            mFragmentContainer.setVisibility(View.GONE);
        } else {
            mLoadingIndicatorContainer.setVisibility(View.GONE);
            mFragmentContainer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Returns the width (in physical px) to use for the "currently set wallpaper" thumbnail.
     */
    private int getSingleWallpaperImageWidthPx() {
        Point screenSize = ScreenSizeCalculator.getInstance().getScreenSize(
                getWindowManager().getDefaultDisplay());

        int height = getResources().getDimensionPixelSize(
                R.dimen.current_wallpaper_bottom_sheet_thumb_height);
        return height * screenSize.x / screenSize.y;
    }

    /**
     * Enables and populates the "Currently set" wallpaper BottomSheet.
     */
    private void setUpBottomSheet() {
        mBottomSheet.setVisibility(View.VISIBLE);

        if (Flags.skipDailyWallpaperButtonEnabled) {
            // Add "next" icon to the Next Wallpaper button
            Drawable nextWallpaperButtonDrawable = getResources().getDrawable(
                    R.drawable.ic_refresh_18px);

            // This Drawable's state is shared across the app, so make a copy of it before applying a
            // color tint as not to affect other clients elsewhere in the app.
            nextWallpaperButtonDrawable =
                    nextWallpaperButtonDrawable.getConstantState().newDrawable().mutate();
            // Color the "compass" icon with the accent color.
            nextWallpaperButtonDrawable.setColorFilter(
                    getResources().getColor(R.color.accent_color), Mode.SRC_IN);
            ButtonDrawableSetterCompat.setDrawableToButtonStart(
                    mCurrentWallpaperSkipWallpaperButton, nextWallpaperButtonDrawable);
        }

        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior =
                BottomSheetBehavior.from(mBottomSheet);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
            }

            @Override
            public void onSlide(@NonNull View view, float slideOffset) {
                float alpha;
                if (slideOffset >= 0) {
                    alpha = slideOffset;
                } else {
                    alpha = 1f - slideOffset;
                }
                LinearLayout bottomSheetContents = (LinearLayout) findViewById(R.id.bottom_sheet_contents);
                bottomSheetContents.setAlpha(alpha);
            }
        });
    }

    /**
     * Enables a test mode of operation -- in which certain UI features are disabled to allow for
     * UI tests to run correctly. Works around issue in ProgressDialog currently where the dialog
     * constantly keeps the UI thread alive and blocks a test forever.
     */
    void setTestingMode(boolean testingMode) {
        mTestingMode = testingMode;
    }

    /**
     * Obtains the {@link WallpaperInfo} object(s) representing the wallpaper(s) currently set to the
     * device from the {@link CurrentWallpaperInfoFactory} and displays them in the BottomSheet.
     */
    @Override
    public void refreshCurrentWallpapers(@Nullable RefreshListener refreshListener) {
        final Injector injector = InjectorProvider.getInjector();
        final Context appContext = getApplicationContext();

        CurrentWallpaperInfoFactory factory = injector.getCurrentWallpaperFactory(this);
        factory.createCurrentWallpaperInfos(new WallpaperInfoCallback() {
            @Override
            public void onWallpaperInfoCreated(
                    final WallpaperInfo homeWallpaper,
                    @Nullable final WallpaperInfo lockWallpaper,
                    @PresentationMode final int presentationMode) {

                if (isDestroyed()) {
                    return;
                }

                // Fetch the home wallpaper's thumbnail asset asynchronously to work around expensive
                // method call to WallpaperManager#getWallpaperFile made from the CurrentWallpaperInfoVN
                // getAsset() method.
                AssetReceiver assetReceiver = (Asset thumbAsset) -> {
                    if (isDestroyed()) {
                        return;
                    }

                    homeWallpaper.getThumbAsset(appContext).loadDrawableWithTransition(
                            TopLevelPickerActivity.this,
                            mCurrentWallpaperImage,
                            200 /* transitionDurationMillis */,
                            () -> {
                                if (refreshListener != null) {
                                    refreshListener.onCurrentWallpaperRefreshed();
                                }
                            },
                            Color.TRANSPARENT);
                };
                new FetchThumbAssetTask(appContext, homeWallpaper, assetReceiver).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR);

                mCurrentWallpaperPresentationMode.setText(
                        AttributionFormatter.getHumanReadableWallpaperPresentationMode(
                                TopLevelPickerActivity.this, presentationMode));

                List<String> attributions = homeWallpaper.getAttributions(appContext);
                if (attributions.size() > 0 && attributions.get(0) != null) {
                    mCurrentWallpaperTitle.setText(attributions.get(0));
                }

                mCurrentWallpaperSubtitle.setText(
                        AttributionFormatter.formatWallpaperSubtitle(appContext, homeWallpaper));

                final String actionUrl = homeWallpaper.getActionUrl(appContext);
                if (actionUrl != null && !actionUrl.isEmpty()) {
                    Uri exploreUri = Uri.parse(actionUrl);

                    ExploreIntentChecker intentChecker = injector.getExploreIntentChecker(appContext);
                    intentChecker.fetchValidActionViewIntent(exploreUri, (@Nullable Intent exploreIntent) -> {
                        if (exploreIntent != null && !isDestroyed()) {
                            // Set the icon for the button
                            Drawable exploreButtonDrawable = getResources().getDrawable(
                                    homeWallpaper.getActionIconRes(appContext));

                            // This Drawable's state is shared across the app, so make a copy of it
                            // before applying a color tint as not to affect other clients elsewhere
                            // in the app.
                            exploreButtonDrawable = exploreButtonDrawable.getConstantState()
                                    .newDrawable().mutate();
                            // Color the "compass" icon with the accent color.
                            exploreButtonDrawable.setColorFilter(
                                    getResources().getColor(R.color.accent_color), Mode.SRC_IN);

                            ButtonDrawableSetterCompat.setDrawableToButtonStart(
                                    mCurrentWallpaperExploreButton, exploreButtonDrawable);
                            mCurrentWallpaperExploreButton.setText(getString(
                                    homeWallpaper.getActionLabelRes(appContext)));
                            mCurrentWallpaperExploreButton.setVisibility(View.VISIBLE);
                            mCurrentWallpaperExploreButton.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mUserEventLogger.logExploreClicked(
                                            homeWallpaper.getCollectionId(appContext));
                                    startActivity(exploreIntent);
                                }
                            });
                        }
                    });
                } else {
                    mCurrentWallpaperExploreButton.setVisibility(View.GONE);
                }

                // Hide the wallpaper position options UI if the current home wallpaper is not from
                // "my photos".
                String homeCollectionId = homeWallpaper.getCollectionId(TopLevelPickerActivity.this);
                if (mWallpaperPositionOptions != null
                        && homeCollectionId != null // May be null if app is being used for the first time.
                        && !homeCollectionId.equals(getString(R.string.image_wallpaper_collection_id))) {
                    mWallpaperPositionOptions.setVisibility(View.GONE);
                }

                boolean showSkipWallpaperButton = Flags.skipDailyWallpaperButtonEnabled
                        && presentationMode == WallpaperPreferences.PRESENTATION_MODE_ROTATING;
                if (showSkipWallpaperButton) {
                    mCurrentWallpaperSkipWallpaperButton.setVisibility(View.VISIBLE);
                    mCurrentWallpaperSkipWallpaperButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            refreshDailyWallpaper();
                        }
                    });
                } else {
                    mCurrentWallpaperSkipWallpaperButton.setVisibility(View.GONE);
                }

                if (refreshListener != null) {
                    refreshListener.onCurrentWallpaperRefreshed();
                }
            }
        }, true /* forceRefresh */);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        FormFactorChecker formFactorChecker = InjectorProvider.getInjector().getFormFactorChecker(this);
        if (formFactorChecker.getFormFactor() == FormFactorChecker.FORM_FACTOR_DESKTOP) {
            TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);

            // tabLayout is only present when the main IndividualPickerFragment is present (as opposed to
            // the WallpaperDisabledFragment), so need this null check.
            if (tabLayout != null) {
                savedInstanceState.putInt(KEY_SELECTED_CATEGORY_TAB, tabLayout.getSelectedTabPosition());
            }
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Populates the categories appropriately depending on the device form factor.
     *
     * @param selectedTabPosition The position of the tab to show as selected, or -1 if no tab
     *                            should be selected (i.e. because there is no tab layout present, as on MOBILE form factor).
     * @param forceRefresh        Whether to force a refresh of categories from the CategoryProvider. True if
     *                            on first launch.
     */
    private void populateCategories(final int selectedTabPosition, boolean forceRefresh) {

        final CategoryPickerFragment categoryPickerFragment = getCategoryPickerFragment();

        if (forceRefresh && categoryPickerFragment != null) {
            categoryPickerFragment.clearCategories();
        }

        mCategoryProvider.fetchCategories(new CategoryReceiver() {
            @Override
            public void onCategoryReceived(Category category) {
                addCategory(category, true);
            }

            @Override
            public void doneFetchingCategories() {
                if (mFormFactor == FormFactorChecker.FORM_FACTOR_MOBILE) {
                    notifyDoneFetchingCategories();
                } else { // DESKTOP
                    populateCategoryTabs(selectedTabPosition);
                }
            }
        }, forceRefresh);
    }

    private void notifyDoneFetchingCategories() {
        CategoryPickerFragment categoryPickerFragment = getCategoryPickerFragment();
        if (categoryPickerFragment != null) {
            categoryPickerFragment.doneFetchingCategories();
        }
    }

    private void addCategory(Category category, boolean fetchingAll) {
        CategoryPickerFragment categoryPickerFragment = getCategoryPickerFragment();
        if (categoryPickerFragment != null) {
            categoryPickerFragment.addCategory(category, fetchingAll);
        }
    }

    private void removeCategory(Category category) {
        CategoryPickerFragment categoryPickerFragment = getCategoryPickerFragment();
        if (categoryPickerFragment != null) {
            categoryPickerFragment.removeCategory(category);
        }
    }

    private void updateCategory(Category category) {
        CategoryPickerFragment categoryPickerFragment = getCategoryPickerFragment();
        if (categoryPickerFragment != null) {
            categoryPickerFragment.updateCategory(category);
        }
    }

    @Nullable
    private CategoryPickerFragment getCategoryPickerFragment() {
        if (mFormFactor != FormFactorChecker.FORM_FACTOR_MOBILE) {
            return null;
        }
        FragmentManager fm = getSupportFragmentManager();
        return (CategoryPickerFragment) fm.findFragmentById(R.id.fragment_container);
    }

    /**
     * Populates the category tabs on DESKTOP form factor.
     *
     * @param selectedTabPosition The position of the tab to show as selected, or -1 if no particular
     *                            tab should be selected (in which case: the tab of the category for the currently set
     *                            wallpaper will be selected if enumerable; if not, the first enumerable category's tab will
     *                            be selected).
     */
    private void populateCategoryTabs(int selectedTabPosition) {
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.removeAllTabs();

        String currentlySetCollectionId = mPreferences.getHomeWallpaperCollectionId();

        Tab tabToSelect = null;
        Tab firstEnumerableCategoryTab = null;
        for (int i = 0; i < mCategoryProvider.getSize(); i++) {
            Category category = mCategoryProvider.getCategory(i);

            Tab tab = tabLayout.newTab();
            tab.setText(category.getTitle());
            tab.setTag(category);
            tabLayout.addTab(tab, false /* setSelected */);

            if (firstEnumerableCategoryTab == null && category.isEnumerable()) {
                firstEnumerableCategoryTab = tab;
            }

            boolean shouldSelectTab = (i == selectedTabPosition)
                    || (selectedTabPosition == -1
                    && tabToSelect == null
                    && category.isEnumerable()
                    && currentlySetCollectionId != null
                    && currentlySetCollectionId.equals(category.getCollectionId()));

            if (shouldSelectTab) {
                tabToSelect = tab;
            }
        }

        // If the above loop did not identify a specific tab to select, then just select the tab for
        // the first enumerable category.
        if (tabToSelect == null) {
            tabToSelect = firstEnumerableCategoryTab;
        }

        // There may be no enumerable tabs (e.g., offline case), so we need to null-check again.
        if (tabToSelect != null) {
            tabToSelect.select();
        }
    }

    /**
     * Refreshes the current wallpaper in a daily wallpaper rotation.
     */
    private void refreshDailyWallpaper() {
        // ProgressDialog endlessly updates the UI thread, keeping it from going idle which therefore
        // causes Espresso to hang once the dialog is shown.
        if (!mTestingMode) {
            int themeResId;
            if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
                themeResId = R.style.ProgressDialogThemePreL;
            } else {
                themeResId = R.style.LightDialogTheme;
            }
            mRefreshWallpaperProgressDialog = new ProgressDialog(this, themeResId);
            mRefreshWallpaperProgressDialog.setTitle(null);
            mRefreshWallpaperProgressDialog.setMessage(
                    getResources().getString(R.string.refreshing_daily_wallpaper_dialog_message));
            mRefreshWallpaperProgressDialog.setIndeterminate(true);
            mRefreshWallpaperProgressDialog.show();
        }

        WallpaperRotationRefresher wallpaperRotationRefresher =
                InjectorProvider.getInjector().getWallpaperRotationRefresher();
        wallpaperRotationRefresher.refreshWallpaper(this, new Listener() {
            @Override
            public void onRefreshed() {
                if (isDestroyed()) {
                    return;
                }

                if (mRefreshWallpaperProgressDialog != null) {
                    mRefreshWallpaperProgressDialog.dismiss();
                }

                refreshCurrentWallpapers(null /* refreshListener */);
            }

            @Override
            public void onError() {
                if (mRefreshWallpaperProgressDialog != null) {
                    mRefreshWallpaperProgressDialog.dismiss();
                }

                AlertDialog errorDialog = new AlertDialog.Builder(
                        TopLevelPickerActivity.this, R.style.LightDialogTheme)
                        .setMessage(R.string.refresh_daily_wallpaper_failed_message)
                        .setPositiveButton(android.R.string.ok, null /* onClickListener */)
                        .create();
                errorDialog.show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SHOW_CATEGORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = (data == null) ? null : data.getData();
            if (imageUri != null) {
                // User selected an image from the system picker, so launch the preview for that image.
                ImageWallpaperInfo imageWallpaper = new ImageWallpaperInfo(imageUri);
                if (mFormFactor == FormFactorChecker.FORM_FACTOR_DESKTOP) {
                    setCustomPhotoWallpaper(imageWallpaper);
                    return;
                }

                imageWallpaper.showPreview(this, mPreviewIntentFactory, PREVIEW_WALLPAPER_REQUEST_CODE);
            } else {
                // User finished viewing a category without any data, which implies that the user previewed
                // and selected a wallpaper in-app, so finish this activity.
                finishActivityWithResultOk();
            }
        } else if (requestCode == PREVIEW_WALLPAPER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // User previewed and selected a wallpaper, so finish this activity.
            finishActivityWithResultOk();
        }
    }

    /**
     * Shows the view-only preview activity for the given wallpaper.
     */
    public void showViewOnlyPreview(WallpaperInfo wallpaperInfo) {
        wallpaperInfo.showPreview(
                this, mViewOnlyPreviewIntentFactory, VIEW_ONLY_PREVIEW_WALLPAPER_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE
                && permissions.length > 0
                && permissions[0].equals(permission.READ_EXTERNAL_STORAGE)
                && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                for (PermissionChangedListener listener : mPermissionChangedListeners) {
                    listener.onPermissionsGranted();
                }
            } else if (!shouldShowRequestPermissionRationale(permission.READ_EXTERNAL_STORAGE)) {
                for (PermissionChangedListener listener : mPermissionChangedListeners) {
                    listener.onPermissionsDenied(true /* dontAskAgain */);
                }
            } else {
                for (PermissionChangedListener listener : mPermissionChangedListeners) {
                    listener.onPermissionsDenied(false /* dontAskAgain */);
                }
            }
        }
        mPermissionChangedListeners.clear();
    }

    /**
     * Shows the picker activity for the given category.
     */
    public void show(String collectionId) {
        Category category = findCategoryForCollectionId(collectionId);
        if (category == null) {
            return;
        }

        if (mFormFactor == FormFactorChecker.FORM_FACTOR_MOBILE) {
            category.show(this, mPickerIntentFactory, SHOW_CATEGORY_REQUEST_CODE);
        } else { // DESKTOP
            showCategoryDesktop(collectionId);
        }
    }

    private void reselectLastTab() {
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        // In the offline case, "My photos" could be the only category. Thus we need this check --
        // to ensure that we don't try to select the "previously selected" category which was -1.
        if (mLastSelectedCategoryTabIndex > -1) {
            Tab tabToSelect = tabLayout.getTabAt(mLastSelectedCategoryTabIndex);
            if (((Category) tabToSelect.getTag()).isEnumerable()) {
                tabToSelect.select();
            }
        }
    }

    @Nullable
    private Category findCategoryForCollectionId(String collectionId) {
        return mCategoryProvider.getCategory(collectionId);
    }

    private void showCategoryDesktop(String collectionId) {
        Category category = findCategoryForCollectionId(collectionId);
        if (category == null) {
            return;
        }

        if (category.isEnumerable()) {
            // Replace contained IndividualPickerFragment with a new instance for the given category.
            final FragmentManager fm = getSupportFragmentManager();
            Fragment fragment = fm.findFragmentById(R.id.fragment_container);
            if (fragment != null) {
                fm.beginTransaction()
                        .remove(fragment)
                        .commit();
            }
            IndividualPickerFragment newFragment = IndividualPickerFragment.newInstance(collectionId);
            fm.beginTransaction()
                    .add(R.id.fragment_container, newFragment)
                    .commit();
            newFragment.setCurrentWallpaperBottomSheetPresenter(this);
            newFragment.setWallpapersUiContainer(this);
        } else {
            category.show(this, mPickerIntentFactory, SHOW_CATEGORY_REQUEST_CODE);

            // Need to select the tab here in case we are coming back from a "My photos" in which case
            // the tab would have been set to "My photos" while viewing a regular image category.
            reselectLastTab();
        }
    }

    private void finishActivityWithResultOk() {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setResult(Activity.RESULT_OK);
        finish();
    }

    @WallpaperSupportLevel
    private int getWallpaperSupportLevel() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);

        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            if (wallpaperManager.isWallpaperSupported()) {
                return wallpaperManager.isSetWallpaperAllowed()
                        ? WallpaperDisabledFragment.SUPPORTED_CAN_SET
                        : WallpaperDisabledFragment.NOT_SUPPORTED_BLOCKED_BY_ADMIN;
            }
            return WallpaperDisabledFragment.NOT_SUPPORTED_BY_DEVICE;
        } else if (VERSION.SDK_INT >= VERSION_CODES.M) {
            return wallpaperManager.isWallpaperSupported() ? WallpaperDisabledFragment.SUPPORTED_CAN_SET
                    : WallpaperDisabledFragment.NOT_SUPPORTED_BY_DEVICE;
        } else {
            WallpaperManagerCompat wallpaperManagerCompat =
                    InjectorProvider.getInjector().getWallpaperManagerCompat(this);
            boolean isSupported = wallpaperManagerCompat.getDrawable() != null;
            wallpaperManager.forgetLoadedWallpaper();
            return isSupported ? WallpaperDisabledFragment.SUPPORTED_CAN_SET
                    : WallpaperDisabledFragment.NOT_SUPPORTED_BY_DEVICE;
        }
    }

    @Override
    public void setCurrentWallpapersExpanded(boolean expanded) {
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior =
                BottomSheetBehavior.from(mBottomSheet);
        bottomSheetBehavior.setState(
                expanded ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public void onWallpapersReady() {
        setDesktopLoading(false);
        setCurrentWallpapersExpanded(true);
    }

    @Override
    public void onClickTryAgain(@Destination int unused) {
        // Retry the set wallpaper operation with the default center-crop setting.
        if (mPendingSetWallpaperInfo != null) {
            setCustomPhotoWallpaper(mPendingSetWallpaperInfo);
        }
    }

    /**
     * Sets the provides wallpaper to the device with center-cropped and scaled to fit the device's
     * default display.
     */
    private void setCustomPhotoWallpaper(final WallpaperInfo wallpaper) {
        // Save this WallpaperInfo so we can retry this operation later if it fails.
        mPendingSetWallpaperInfo = wallpaper;

        showSettingWallpaperProgressDialog();

        mWallpaperPersister.setIndividualWallpaperWithPosition(this, wallpaper,
                WallpaperPersister.WALLPAPER_POSITION_CENTER_CROP, new SetWallpaperCallback() {
                    @Override
                    public void onSuccess() {
                        dismissSettingWallpaperProgressDialog();
                        refreshCurrentWallpapers(null /* refreshListener */);

                        mPreferences.setPendingWallpaperSetStatus(
                                WallpaperPreferences.WALLPAPER_SET_NOT_PENDING);
                        mUserEventLogger.logWallpaperSet(
                                wallpaper.getCollectionId(getApplicationContext()),
                                wallpaper.getWallpaperId());
                        mUserEventLogger.logWallpaperSetResult(UserEventLogger.WALLPAPER_SET_RESULT_SUCCESS);

                        // The user may have closed the activity before the set wallpaper operation completed.
                        if (isDestroyed()) {
                            return;
                        }

                        // Show the wallpaper crop option selector and bind click event handlers.
                        mWallpaperPositionOptions.setVisibility(View.VISIBLE);

                        mWasCustomPhotoWallpaperSet = true;
                        mCustomPhotoWallpaperPosition = WallpaperPersister.WALLPAPER_POSITION_CENTER_CROP;

                        initializeWallpaperPositionOptionClickHandlers(wallpaper);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        dismissSettingWallpaperProgressDialog();
                        showSetWallpaperErrorDialog();

                        mPreferences.setPendingWallpaperSetStatus(
                                WallpaperPreferences.WALLPAPER_SET_NOT_PENDING);
                        mUserEventLogger.logWallpaperSetResult(
                                UserEventLogger.WALLPAPER_SET_RESULT_FAILURE);
                        @WallpaperSetFailureReason int failureReason = ThrowableAnalyzer.isOOM(throwable)
                                ? UserEventLogger.WALLPAPER_SET_FAILURE_REASON_OOM
                                : UserEventLogger.WALLPAPER_SET_FAILURE_REASON_OTHER;
                        mUserEventLogger.logWallpaperSetFailureReason(failureReason);
                        Log.e(TAG, "Unable to set wallpaper from 'my photos'.");
                    }
                });
    }

    /**
     * Initializes the wallpaper position button click handlers to change the way the provided
     * wallpaper is set to the device.
     */
    private void initializeWallpaperPositionOptionClickHandlers(final WallpaperInfo wallpaperInfo) {
        Button centerCropOptionBtn = (Button) findViewById(R.id.wallpaper_position_option_center_crop);
        Button stretchOptionBtn = (Button) findViewById(R.id.wallpaper_position_option_stretched);
        Button centerOptionBtn = (Button) findViewById(R.id.wallpaper_position_option_center);

        // The "center crop" wallpaper position button is selected by default.
        setCenterCropWallpaperPositionButtonSelected(centerCropOptionBtn, true /* isSelected */);
        centerCropOptionBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mWallpaperPersister.setIndividualWallpaperWithPosition(TopLevelPickerActivity.this,
                        wallpaperInfo, WallpaperPersister.WALLPAPER_POSITION_CENTER_CROP,
                        new SetWallpaperCallback() {
                            @Override
                            public void onSuccess() {
                                // The user may have closed the activity before the set wallpaper operation
                                // completed.
                                if (isDestroyed()) {
                                    return;
                                }

                                refreshCurrentWallpapers(null /* refreshListener */);

                                setCenterCropWallpaperPositionButtonSelected(
                                        centerCropOptionBtn, true /* isSelected */);
                                setCenterWallpaperPositionButtonSelected(centerOptionBtn, false /* isSelected */);
                                setStretchWallpaperPositionButtonSelected(stretchOptionBtn, false /* isSelected */);

                                mCustomPhotoWallpaperPosition = WallpaperPersister.WALLPAPER_POSITION_CENTER_CROP;
                            }

                            @Override
                            public void onError(@Nullable Throwable throwable) {
                                // no-op
                            }
                        });
            }
        });

        // "Stretch" is not selected by default.
        setStretchWallpaperPositionButtonSelected(stretchOptionBtn, false /* isSelected */);
        stretchOptionBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mWallpaperPersister.setIndividualWallpaperWithPosition(TopLevelPickerActivity.this,
                        wallpaperInfo, WallpaperPersister.WALLPAPER_POSITION_STRETCH,
                        new SetWallpaperCallback() {
                            @Override
                            public void onSuccess() {
                                // The user may have closed the activity before the set wallpaper operation
                                // completed.
                                if (isDestroyed()) {
                                    return;
                                }

                                refreshCurrentWallpapers(null /* refreshListener */);

                                setStretchWallpaperPositionButtonSelected(stretchOptionBtn, true /* isSelected */);
                                setCenterCropWallpaperPositionButtonSelected(
                                        centerCropOptionBtn, false /* isSelected */);
                                setCenterWallpaperPositionButtonSelected(centerOptionBtn, false /* isSelected */);

                                mCustomPhotoWallpaperPosition = WallpaperPersister.WALLPAPER_POSITION_STRETCH;
                            }

                            @Override
                            public void onError(@Nullable Throwable throwable) {
                                // no-op
                            }
                        });
            }
        });

        // "Center" is not selected by default.
        setCenterWallpaperPositionButtonSelected(centerOptionBtn, false /* isSelected */);
        centerOptionBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mWallpaperPersister.setIndividualWallpaperWithPosition(TopLevelPickerActivity.this,
                        wallpaperInfo, WallpaperPersister.WALLPAPER_POSITION_CENTER,
                        new SetWallpaperCallback() {
                            @Override
                            public void onSuccess() {
                                // The user may have closed the activity before the set wallpaper operation
                                // completed.
                                if (isDestroyed()) {
                                    return;
                                }

                                refreshCurrentWallpapers(null /* refreshListener */);

                                setCenterWallpaperPositionButtonSelected(centerOptionBtn, true /* isSelected */);
                                setCenterCropWallpaperPositionButtonSelected(
                                        centerCropOptionBtn, false /* isSelected */);
                                setStretchWallpaperPositionButtonSelected(stretchOptionBtn, false /* isSelected */);

                                mCustomPhotoWallpaperPosition = WallpaperPersister.WALLPAPER_POSITION_CENTER;
                            }

                            @Override
                            public void onError(@Nullable Throwable throwable) {
                                // no-op
                            }
                        });
            }
        });
    }

    private void setCenterWallpaperPositionButtonSelected(Button button, boolean isSelected) {
        int drawableId = isSelected ? R.drawable.center_blue : R.drawable.center_grey;
        ButtonDrawableSetterCompat.setDrawableToButtonStart(button, getDrawable(drawableId));
        button.setTextColor(getColor(getTextColorIdForWallpaperPositionButton(isSelected)));
    }

    private void setCenterCropWallpaperPositionButtonSelected(Button button, boolean isSelected) {
        int drawableId = isSelected ? R.drawable.center_crop_blue : R.drawable.center_crop_grey;
        ButtonDrawableSetterCompat.setDrawableToButtonStart(button, getDrawable(drawableId));
        button.setTextColor(getColor(getTextColorIdForWallpaperPositionButton(isSelected)));
    }

    private void setStretchWallpaperPositionButtonSelected(Button button, boolean isSelected) {
        int drawableId = isSelected ? R.drawable.stretch_blue : R.drawable.stretch_grey;
        ButtonDrawableSetterCompat.setDrawableToButtonStart(button, getDrawable(drawableId));
        button.setTextColor(getColor(getTextColorIdForWallpaperPositionButton(isSelected)));
    }

    private void showSettingWallpaperProgressDialog() {
        // ProgressDialog endlessly updates the UI thread, keeping it from going idle which therefore
        // causes Espresso to hang once the dialog is shown.
        if (!mTestingMode) {
            int themeResId;
            if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
                themeResId = R.style.ProgressDialogThemePreL;
            } else {
                themeResId = R.style.LightDialogTheme;
            }
            mSetWallpaperProgressDialog = new ProgressDialog(this, themeResId);
            mSetWallpaperProgressDialog.setTitle(null);
            mSetWallpaperProgressDialog.setMessage(
                    getResources().getString(R.string.set_wallpaper_progress_message));
            mSetWallpaperProgressDialog.setIndeterminate(true);
            mSetWallpaperProgressDialog.show();
        }
    }

    private void dismissSettingWallpaperProgressDialog() {
        if (mSetWallpaperProgressDialog != null) {
            mSetWallpaperProgressDialog.dismiss();
        }
    }

    private void showSetWallpaperErrorDialog() {
        SetWallpaperErrorDialogFragment dialogFragment = SetWallpaperErrorDialogFragment.newInstance(
                R.string.set_wallpaper_error_message, WallpaperPersister.DEST_BOTH);

        if (isSafeToCommitFragmentTransaction()) {
            dialogFragment.show(getSupportFragmentManager(), TAG_SET_WALLPAPER_ERROR_DIALOG_FRAGMENT);
        } else {
            mStagedSetWallpaperErrorDialogFragment = dialogFragment;
        }
    }

    private interface AssetReceiver {
        void onAssetReceived(Asset asset);
    }

    /**
     * An AsyncTask for asynchronously fetching the thumbnail asset for a given WallpaperInfo.
     * Used to work around expensive method call to WallpaperManager#getWallpaperFile made from the
     * CurrentWallpaperInfoVN getAsset() method.
     */
    private static class FetchThumbAssetTask extends AsyncTask<Void, Void, Asset> {
        private Context mAppContext;
        private WallpaperInfo mWallpaperInfo;
        private AssetReceiver mReceiver;

        public FetchThumbAssetTask(Context appContext, WallpaperInfo wallpaperInfo,
                                   AssetReceiver receiver) {
            mAppContext = appContext;
            mWallpaperInfo = wallpaperInfo;
            mReceiver = receiver;
        }

        @Override
        protected Asset doInBackground(Void... params) {
            return mWallpaperInfo.getThumbAsset(mAppContext);
        }

        @Override
        protected void onPostExecute(Asset thumbAsset) {
            mReceiver.onAssetReceived(thumbAsset);
        }
    }
}
