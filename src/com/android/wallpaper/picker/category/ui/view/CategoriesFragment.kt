/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.ImageWallpaperInfo
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.picker.AppbarFragment
import com.android.wallpaper.picker.MyPhotosStarter
import com.android.wallpaper.picker.WallpaperPickerDelegate.VIEW_ONLY_PREVIEW_WALLPAPER_REQUEST_CODE
import com.android.wallpaper.picker.category.ui.binder.BannerProvider
import com.android.wallpaper.picker.category.ui.binder.CategoriesBinder
import com.android.wallpaper.picker.category.ui.view.providers.IndividualPickerFactory
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel
import com.android.wallpaper.picker.common.preview.data.repository.PersistentWallpaperModelRepository
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import com.android.wallpaper.util.ActivityUtils
import com.android.wallpaper.util.SizeCalculator
import com.android.wallpaper.util.converter.WallpaperModelFactory
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** This fragment displays the user interface for the categories */
@AndroidEntryPoint(AppbarFragment::class)
class CategoriesFragment : Hilt_CategoriesFragment() {

    @Inject lateinit var individualPickerFactory: IndividualPickerFactory
    @Inject lateinit var persistentWallpaperModelRepository: PersistentWallpaperModelRepository
    @Inject lateinit var multiPanesChecker: MultiPanesChecker
    @Inject lateinit var myPhotosStarterImpl: MyPhotosStarterImpl
    @Inject lateinit var wallpaperModelFactory: WallpaperModelFactory
    @Inject lateinit var colorUpdateViewModel: ColorUpdateViewModel
    @Inject lateinit var bannerProvider: BannerProvider
    private lateinit var photoPickerLauncher: ActivityResultLauncher<Intent>

    // TODO: this may need to be scoped to fragment if the architecture changes
    private val categoriesViewModel by activityViewModels<CategoriesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != Activity.RESULT_OK) {
                    return@registerForActivityResult
                }

                val data: Intent? = result.data
                val imageUri: Uri = data?.data ?: return@registerForActivityResult
                val imageWallpaperInfo = ImageWallpaperInfo(imageUri)
                val context = context ?: return@registerForActivityResult
                val wallpaperModel =
                    wallpaperModelFactory.getWallpaperModel(context, imageWallpaperInfo)
                startWallpaperPreviewActivity(wallpaperModel, false)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view =
            inflater.inflate(R.layout.categories_fragment, container, /* attachToRoot= */ false)

        setUpToolbar(view)
        setTitle(getText(R.string.wallpaper_title))

        val isNewPickerUi = BaseFlags.get().isNewPickerUi()
        if (isNewPickerUi) {
            ColorUpdateBinder.bind(
                setColor = { _ ->
                    // There is no way to programmatically set app:liftOnScrollColor in
                    // AppBarLayout, therefore remove and re-add view to update colors based on new
                    // context
                    val contentParent = view.requireViewById<ViewGroup>(R.id.content_parent)
                    val appBarLayout = view.requireViewById<AppBarLayout>(R.id.app_bar)
                    contentParent.removeView(appBarLayout)
                    layoutInflater.inflate(R.layout.section_header_content, contentParent, true)
                    setUpToolbar(view)
                    setTitle(getText(R.string.wallpaper_title))
                    contentParent.requestApplyInsets()
                },
                color = colorUpdateViewModel.colorSurfaceContainer,
                shouldAnimate = { false },
                lifecycleOwner = viewLifecycleOwner,
            )
        }

        CategoriesBinder.bind(
            categoriesPage = view.requireViewById<RecyclerView>(R.id.content_parent),
            viewModel = categoriesViewModel,
            windowWidth = SizeCalculator.getActivityWindowWidthPx(this.activity),
            colorUpdateViewModel = colorUpdateViewModel,
            shouldAnimateColor = { false },
            bannerProvider = bannerProvider,
            lifecycleOwner = viewLifecycleOwner,
        ) { navigationEvent, callback ->
            when (navigationEvent) {
                is CategoriesViewModel.NavigationEvent.NavigateToWallpaperCollection -> {
                    switchFragment(
                        individualPickerFactory.getIndividualPickerInstance(
                            navigationEvent.categoryId,
                            navigationEvent.categoryType,
                        )
                    )
                }
                is CategoriesViewModel.NavigationEvent.NavigateToPhotosPicker -> {
                    if (BaseFlags.get().isPhotoPickerEnabled()) {
                        parentFragmentManager.commit {
                            replace<PhotoPickerFragment>(R.id.fragment_container)
                            addToBackStack(null)
                        }
                    } else {
                        // make call to permission handler to grab photos and pass callback
                        myPhotosStarterImpl.requestCustomPhotoPicker(
                            object : MyPhotosStarter.PermissionChangedListener {
                                override fun onPermissionsGranted() {
                                    callback?.invoke()
                                }

                                override fun onPermissionsDenied(dontAskAgain: Boolean) {
                                    if (dontAskAgain) {
                                        showPermissionSnackbar()
                                    }
                                }
                            },
                            requireActivity(),
                            photoPickerLauncher,
                        )
                    }
                }
                is CategoriesViewModel.NavigationEvent.NavigateToThirdParty -> {
                    startThirdPartyCategoryActivity(
                        requireActivity(),
                        SHOW_CATEGORY_REQUEST_CODE,
                        navigationEvent.resolveInfo,
                    )
                }
                is CategoriesViewModel.NavigationEvent.NavigateToPreviewScreen -> {
                    startWallpaperPreviewActivity(
                        navigationEvent.wallpaperModel,
                        navigationEvent.categoryType ==
                            CategoriesViewModel.CategoryType.CreativeCategories,
                    )
                }
            }
        }
        return view
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
            )
        ActivityUtils.startActivityForResultSafely(
            activity,
            previewIntent,
            VIEW_ONLY_PREVIEW_WALLPAPER_REQUEST_CODE, // TODO: provide correct request code
        )
    }

    private fun showPermissionSnackbar() {
        val snackbar =
            Snackbar.make(
                requireView(),
                R.string.settings_snackbar_description,
                Snackbar.LENGTH_LONG,
            )
        val layout = snackbar.view as Snackbar.SnackbarLayout
        val textView =
            layout.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
        layout.setBackgroundResource(R.drawable.snackbar_background)

        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.system_on_primary))
        snackbar.setActionTextColor(
            ContextCompat.getColor(requireContext(), R.color.system_surface_container)
        )
        snackbar.setAction(requireContext().getString(R.string.settings_snackbar_enable)) {
            startSettings(SETTINGS_APP_INFO_REQUEST_CODE)
        }
        snackbar.show()
    }

    private fun startSettings(resultCode: Int) {
        val activity = activity ?: return
        val appInfoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, /* fragment= */ null)
        appInfoIntent.setData(uri)
        startActivityForResult(appInfoIntent, resultCode)
    }

    private fun startThirdPartyCategoryActivity(
        srcActivity: Activity,
        requestCode: Int,
        resolveInfo: ResolveInfo,
    ) {
        val itemComponentName =
            ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
        val launchIntent = Intent(Intent.ACTION_SET_WALLPAPER)
        launchIntent.component = itemComponentName
        ActivityUtils.startActivityForResultSafely(srcActivity, launchIntent, requestCode)
    }

    private fun switchFragment(fragment: Fragment) {
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
        parentFragmentManager.executePendingTransactions()
    }

    companion object {
        const val SHOW_CATEGORY_REQUEST_CODE = 0
        const val SETTINGS_APP_INFO_REQUEST_CODE = 1
        const val READ_IMAGE_PERMISSION: String = Manifest.permission.READ_MEDIA_IMAGES
    }
}
