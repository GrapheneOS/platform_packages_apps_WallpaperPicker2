/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.wallpaper.picker.category.ui.view

import android.annotation.NonNull
import android.annotation.RequiresApi
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.photopicker.EmbeddedPhotoPickerClient
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import android.widget.photopicker.EmbeddedPhotoPickerProvider
import android.widget.photopicker.EmbeddedPhotoPickerProviderFactory
import android.widget.photopicker.EmbeddedPhotoPickerSession
import com.android.wallpaper.R
import com.android.wallpaper.model.ImageWallpaperInfo
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.WallpaperPickerDelegate.VIEW_ONLY_PREVIEW_WALLPAPER_REQUEST_CODE
import com.android.wallpaper.picker.common.preview.data.repository.PersistentWallpaperModelRepository
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import com.android.wallpaper.util.ActivityUtils
import com.android.wallpaper.util.converter.WallpaperModelFactory
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * This is the fragment that will host the Android PhotoPicker. Having it in a fragment enables
 * moving back and forth between preview activity and photopicker.
 */
@AndroidEntryPoint(AppbarFragment::class)
class PhotoPickerFragment : Hilt_PhotoPickerFragment() {

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var wallpaperModelFactory: WallpaperModelFactory
    @Inject lateinit var persistentWallpaperModelRepository: PersistentWallpaperModelRepository
    @Inject lateinit var multiPanesChecker: MultiPanesChecker
    @Inject lateinit var colorUpdateViewModel: ColorUpdateViewModel

    private lateinit var embeddedPickerProvider: EmbeddedPhotoPickerProvider
    private lateinit var surfaceView: SurfaceView
    private lateinit var embeddedPhotoPickerFeatureInfo: EmbeddedPhotoPickerFeatureInfo
    private var session: EmbeddedPhotoPickerSession? = null
    private var view: View? = null

    var navigateToExtendedWallpaperEffects: Boolean? = null

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        view = inflater.inflate(R.layout.fragment_photo_picker, container, false)
        navigateToExtendedWallpaperEffects =
            arguments?.getBoolean(ARG_NAVIGATE_TO_EXTENDED_WALLPAPER_EFFECTS) ?: false
        setUpToolbar(view)
        setTitle(getText(R.string.select_a_photo))

        ColorUpdateBinder.bind(
            setColor = { _ ->
                // There is no way to programmatically set app:liftOnScrollColor in
                // AppBarLayout, therefore remove and re-add view to update colors based on new
                // context
                val contentParent = view?.requireViewById<ViewGroup>(R.id.content_parent)
                val appBarLayout = view?.requireViewById<AppBarLayout>(R.id.app_bar)
                contentParent?.removeView(appBarLayout)
                val sectionHeader =
                    layoutInflater.inflate(R.layout.section_header_content, contentParent, false)
                contentParent?.addView(sectionHeader, 0)
                setUpToolbar(contentParent)
                setTitle(getText(R.string.select_a_photo))
                view?.requestApplyInsets()
            },
            color = colorUpdateViewModel.colorSurfaceContainer,
            shouldAnimate = { false },
            lifecycleOwner = viewLifecycleOwner,
        )
        return view
    }

    override fun onConfigurationChanged(@NonNull newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (session != null) {
            session?.notifyConfigurationChanged(newConfig)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            initPhotoPicker()
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun initPhotoPicker() {
        if (view == null) {
            return // Exit if the view is not available
        }

        embeddedPickerProvider = EmbeddedPhotoPickerProviderFactory.create(appContext)
        surfaceView = requireView().findViewById(R.id.surface)
        surfaceView.setZOrderOnTop(true)
        val filter = mutableListOf("image/*")
        embeddedPhotoPickerFeatureInfo =
            EmbeddedPhotoPickerFeatureInfo.Builder()
                .setMaxSelectionLimit(1)
                .setMimeTypes(filter)
                .build()
        val surfaceHolder = surfaceView.holder

        surfaceHolder.addCallback(SurfaceHolderCallback())
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun launchEmbeddedPhotoPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val displayManager =
                appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displayId = displayManager.displays[0].displayId

            // Open a new embedded PhotoPicker session
            surfaceView.hostToken?.let {
                embeddedPickerProvider.openSession(
                    it,
                    displayId,
                    surfaceView.width,
                    surfaceView.height,
                    embeddedPhotoPickerFeatureInfo,
                    Executors.newSingleThreadExecutor(),
                    ClientCallback(),
                )
            }
        }
    }

    private inner class SurfaceHolderCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.d(TAG, "Surface created")
            launchEmbeddedPhotoPicker()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

        // We don't close the connection in onSurfaceDestroyed because it's also called when we
        // click on a photo and it re-directs us to preview activity. Closing connection will
        // prevent us from retaining the state (i.e. scroll state/search state).
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Log.d(TAG, "Surface destroyed")
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private inner class ClientCallback : EmbeddedPhotoPickerClient {
        override fun onSessionOpened(session: EmbeddedPhotoPickerSession) {
            this@PhotoPickerFragment.session = session
            this@PhotoPickerFragment.session?.notifyPhotoPickerExpanded(true)
            this@PhotoPickerFragment.session?.notifyConfigurationChanged(
                appContext.resources.configuration
            )
            surfaceView.setChildSurfacePackage(session.surfacePackage)
            Log.d(TAG, "Embedded PhotoPicker session opened successfully")
        }

        override fun onSessionError(cause: Throwable) {
            session = null
            Log.e(TAG, "Error occurred in Embedded PhotoPicker session", cause)
        }

        override fun onUriPermissionGranted(uris: List<Uri>) {
            if (uris.isEmpty()) {
                return
            }
            val imageWallpaperInfo = ImageWallpaperInfo(uris[0])
            val wallpaperModel =
                context?.let { wallpaperModelFactory.getWallpaperModel(it, imageWallpaperInfo) }

            if (wallpaperModel != null) {
                startWallpaperPreviewActivity(wallpaperModel, false)
            }
            Log.d(TAG, "Uri permission granted for: $uris")
        }

        override fun onUriPermissionRevoked(uris: List<Uri>) {}

        override fun onSelectionComplete() {
            parentFragmentManager.beginTransaction().remove(this@PhotoPickerFragment).commit()
        }
    }

    private fun startWallpaperPreviewActivity(
        wallpaperModel: WallpaperModel,
        isCreativeCategories: Boolean,
    ) {
        val appContext = requireContext()
        val activity = requireActivity()
        persistentWallpaperModelRepository.setWallpaperModel(wallpaperModel)
        val isMultiPanel = multiPanesChecker.isMultiPanesEnabled(appContext)
        val previewIntent =
            WallpaperPreviewActivity.newIntent(
                context = appContext,
                isAssetIdPresent = true,
                isViewAsHome = true,
                isNewTask = isMultiPanel,
                shouldCategoryRefresh = isCreativeCategories,
                shouldNavigateToExtendedWallpaperEffects =
                    navigateToExtendedWallpaperEffects ?: false,
            )
        ActivityUtils.startActivityForResultSafely(
            activity,
            previewIntent,
            VIEW_ONLY_PREVIEW_WALLPAPER_REQUEST_CODE,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        session?.let {
            it.close()
            session = null
        }
    }

    companion object {
        private const val TAG = "PhotoPickerFragment"

        private const val ARG_NAVIGATE_TO_EXTENDED_WALLPAPER_EFFECTS =
            "navigate_to_extended_wallpaper_effects"

        fun newInstance(shouldNavigateToExtendedWallpaperEffects: Boolean): PhotoPickerFragment {
            val fragment = PhotoPickerFragment()
            val args = Bundle()
            args.putBoolean(
                ARG_NAVIGATE_TO_EXTENDED_WALLPAPER_EFFECTS,
                shouldNavigateToExtendedWallpaperEffects,
            )
            fragment.arguments = args
            return fragment
        }
    }
}
