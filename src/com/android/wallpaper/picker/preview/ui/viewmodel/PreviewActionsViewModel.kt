/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.wallpaper.picker.preview.ui.viewmodel

import android.app.Flags.liveWallpaperContentHandling
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.service.wallpaper.WallpaperSettingsActivity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.android.wallpaper.R
import com.android.wallpaper.effects.Effect
import com.android.wallpaper.effects.EffectsController.EffectEnumInterface
import com.android.wallpaper.model.WallpaperInfoContract.WALLPAPER_DESCRIPTION_CONTENT_HANDLING
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.data.CreativeWallpaperData
import com.android.wallpaper.picker.data.LiveWallpaperData
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_APPLIED
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_APPLY_FAILED
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_APPLY_IN_PROGRESS
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_DISABLE
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_DOWNLOAD_FAILED
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_DOWNLOAD_IN_PROGRESS
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_DOWNLOAD_READY
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus.EFFECT_READY
import com.android.wallpaper.picker.preview.domain.interactor.PreviewActionsInteractor
import com.android.wallpaper.picker.preview.domain.interactor.WallpaperPreviewInteractor
import com.android.wallpaper.picker.preview.shared.model.DownloadStatus
import com.android.wallpaper.picker.preview.shared.model.ImageEffectsModel
import com.android.wallpaper.picker.preview.ui.util.LiveWallpaperDeleteUtil
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.CUSTOMIZE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DELETE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DOWNLOAD
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EDIT
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EFFECTS
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.INFORMATION
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.SHARE
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.CreativeEffectFloatingSheetViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.CustomizeFloatingSheetViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.ImageEffectFloatingSheetViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.InformationFloatingSheetViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.PreviewFloatingSheetViewModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils.Companion.isExtendedEffectWallpaper
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2.EffectDownloadClickListener
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2.EffectSwitchListener
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2.Status.DOWNLOADING
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2.Status.FAILED
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2.Status.IDLE
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2.Status.PROCESSING
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2.Status.SHOW_DOWNLOAD_BUTTON
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2.Status.SUCCESS
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/** View model for the preview action buttons */
@ViewModelScoped
class PreviewActionsViewModel
@Inject
constructor(
    private val previewActionsInteractor: PreviewActionsInteractor,
    wallpaperPreviewInteractor: WallpaperPreviewInteractor,
    liveWallpaperDeleteUtil: LiveWallpaperDeleteUtil,
    @ApplicationContext private val context: Context,
) {
    private val flags = InjectorProvider.getInjector().getFlags()
    private val extendedWallpaperEffectPkgName =
        context.getString(R.string.extended_wallpaper_effects_package)
    private val extendedWallpaperEffectActivityName =
        context.getString(R.string.extended_wallpaper_effects_activity)

    /** [INFORMATION] */
    private val informationFloatingSheetViewModel: Flow<InformationFloatingSheetViewModel?> =
        previewActionsInteractor.wallpaperModel.map { wallpaperModel ->
            if (wallpaperModel == null || !wallpaperModel.shouldShowInformationFloatingSheet()) {
                null
            } else {
                InformationFloatingSheetViewModel(
                    description =
                        (wallpaperModel as? LiveWallpaperModel)?.liveWallpaperData?.description,
                    attributions = wallpaperModel.commonWallpaperData.attributions,
                    actionUrl =
                        if (wallpaperModel.commonWallpaperData.exploreActionUrl.isNullOrEmpty()) {
                            null
                        } else {
                            wallpaperModel.commonWallpaperData.exploreActionUrl
                        },
                    actionButtonTitle =
                        (wallpaperModel as? LiveWallpaperModel)
                            ?.liveWallpaperData
                            ?.contextDescription,
                )
            }
        }

    val isInformationVisible: Flow<Boolean> = informationFloatingSheetViewModel.map { it != null }

    private val _isInformationChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isInformationChecked: Flow<Boolean> = _isInformationChecked.asStateFlow()

    val onInformationClicked: Flow<(() -> Unit)?> =
        combine(isInformationVisible, isInformationChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(INFORMATION)
                    }
                    _isInformationChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    /** [DOWNLOAD] */
    val isDownloadVisible: Flow<Boolean> =
        previewActionsInteractor.downloadableWallpaperModel.map {
            it.status == DownloadStatus.READY_TO_DOWNLOAD || it.status == DownloadStatus.DOWNLOADING
        }
    val isDownloading: Flow<Boolean> =
        previewActionsInteractor.downloadableWallpaperModel.map {
            it.status == DownloadStatus.DOWNLOADING
        }
    val isDownloadButtonEnabled: Flow<Boolean> =
        previewActionsInteractor.downloadableWallpaperModel.map {
            it.status == DownloadStatus.READY_TO_DOWNLOAD
        }

    fun downloadWallpaper() {
        previewActionsInteractor.downloadWallpaper()
    }

    /** [DELETE] */
    private val liveWallpaperDeleteIntent: Flow<Intent?> =
        previewActionsInteractor.wallpaperModel.map {
            if (it is LiveWallpaperModel && it.creativeWallpaperData == null && it.canBeDeleted()) {
                liveWallpaperDeleteUtil.getDeleteActionIntent(
                    it.liveWallpaperData.systemWallpaperInfo
                )
            } else {
                null
            }
        }
    private val creativeWallpaperDeleteUri: Flow<Uri?> =
        previewActionsInteractor.wallpaperModel.map {
            val deleteUri = (it as? LiveWallpaperModel)?.creativeWallpaperData?.deleteUri
            if (deleteUri != null && it.canBeDeleted()) {
                deleteUri
            } else {
                null
            }
        }
    val isDeleteVisible: Flow<Boolean> =
        combine(liveWallpaperDeleteIntent, creativeWallpaperDeleteUri) { intent, uri ->
            intent != null || uri != null
        }

    private val _isDeleteChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDeleteChecked: Flow<Boolean> = _isDeleteChecked.asStateFlow()

    // View model for delete confirmation dialog. Note that null means the dialog should show;
    // otherwise, the dialog should hide.
    val deleteConfirmationDialogViewModel: Flow<DeleteConfirmationDialogViewModel?> =
        combine(isDeleteChecked, liveWallpaperDeleteIntent, creativeWallpaperDeleteUri) {
            isChecked,
            intent,
            uri ->
            if (isChecked && (intent != null || uri != null)) {
                DeleteConfirmationDialogViewModel(
                    onDismiss = { _isDeleteChecked.value = false },
                    liveWallpaperDeleteIntent = intent,
                    creativeWallpaperDeleteUri = uri,
                )
            } else {
                null
            }
        }

    val onDeleteClicked: Flow<(() -> Unit)?> =
        combine(isDeleteVisible, isDeleteChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(DELETE)
                    }
                    _isDeleteChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    /** [EDIT] */
    val editIntent: Flow<Intent?> =
        previewActionsInteractor.wallpaperModel.map { model ->
            (model as? LiveWallpaperModel)?.liveWallpaperData?.getEditActivityIntent(false)?.let {
                intent ->
                if (intent.resolveActivityInfo(context.packageManager, 0) != null) intent else null
            }
        }
    val isEditVisible: Flow<Boolean> = editIntent.map { it != null }

    private val _isEditChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEditChecked: Flow<Boolean> = _isEditChecked.asStateFlow()

    /** [CUSTOMIZE] */
    private val customizeFloatingSheetViewModel: Flow<CustomizeFloatingSheetViewModel?> =
        previewActionsInteractor.wallpaperModel.map {
            (it as? LiveWallpaperModel)
                ?.liveWallpaperData
                ?.systemWallpaperInfo
                ?.settingsSliceUri
                ?.let { CustomizeFloatingSheetViewModel(it) }
        }
    val isCustomizeVisible: Flow<Boolean> = customizeFloatingSheetViewModel.map { it != null }

    private val _isCustomizeChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isCustomizeChecked: Flow<Boolean> = _isCustomizeChecked.asStateFlow()

    val onCustomizeClicked: Flow<(() -> Unit)?> =
        combine(isCustomizeVisible, isCustomizeChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(CUSTOMIZE)
                    }
                    _isCustomizeChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    /** [EFFECTS] */
    private val _imageEffectConfirmDownloadDialogViewModel:
        MutableStateFlow<ImageEffectDialogViewModel?> =
        MutableStateFlow(null)
    // View model for the dialog that confirms downloading the effect ML model.
    val imageEffectConfirmDownloadDialogViewModel =
        _imageEffectConfirmDownloadDialogViewModel.asStateFlow()

    private val imageEffectFloatingSheetViewModel: Flow<ImageEffectFloatingSheetViewModel?> =
        combine(previewActionsInteractor.imageEffectsModel, previewActionsInteractor.imageEffect) {
            imageEffectsModel,
            imageEffect ->
            imageEffect?.let {
                if (
                    !flags.isExtendedWallpaperEnabled() &&
                        imageEffectsModel.status == EFFECT_DISABLE
                ) {
                    null
                } else {
                    getImageEffectFloatingSheetViewModel(imageEffect, imageEffectsModel)
                }
            }
        }
    private val _imageEffectConfirmExitDialogViewModel:
        MutableStateFlow<ImageEffectDialogViewModel?> =
        MutableStateFlow(null)
    val imageEffectConfirmExitDialogViewModel = _imageEffectConfirmExitDialogViewModel.asStateFlow()
    val handleOnBackPressed: Flow<(() -> Boolean)?> =
        combine(
            imageEffectFloatingSheetViewModel,
            previewActionsInteractor.imageEffect,
            isDownloading,
        ) { viewModel, effect, isDownloading ->
            when {
                viewModel?.status == DOWNLOADING -> { ->
                        _imageEffectConfirmExitDialogViewModel.value =
                            ImageEffectDialogViewModel(
                                onDismiss = { _imageEffectConfirmExitDialogViewModel.value = null },
                                onContinue = {
                                    // Continue to exit the screen. We should stop downloading.
                                    effect?.let {
                                        previewActionsInteractor.interruptEffectsModelDownload(it)
                                    }
                                },
                            )
                        true
                    }
                isDownloading -> { -> previewActionsInteractor.cancelDownloadWallpaper() }
                else -> null
            }
        }

    private val creativeEffectFloatingSheetViewModel: Flow<CreativeEffectFloatingSheetViewModel?> =
        previewActionsInteractor.creativeEffectsModel.map { creativeEffectsModel ->
            creativeEffectsModel?.let {
                CreativeEffectFloatingSheetViewModel(
                    title = it.title,
                    subtitle = it.subtitle,
                    wallpaperActions = it.actions,
                    wallpaperEffectSwitchListener = { actionPosition ->
                        previewActionsInteractor.turnOnCreativeEffect(actionPosition)
                    },
                )
            }
        }

    private fun getImageEffectFloatingSheetViewModel(
        effect: Effect,
        imageEffectsModel: ImageEffectsModel,
    ): ImageEffectFloatingSheetViewModel {
        val floatingSheetViewStatus =
            when (imageEffectsModel.status) {
                EFFECT_DISABLE -> {
                    FAILED
                }
                EFFECT_READY -> {
                    IDLE
                }
                EFFECT_DOWNLOAD_READY -> {
                    SHOW_DOWNLOAD_BUTTON
                }
                EFFECT_DOWNLOAD_IN_PROGRESS -> {
                    DOWNLOADING
                }
                EFFECT_APPLY_IN_PROGRESS -> {
                    PROCESSING
                }
                EFFECT_APPLIED -> {
                    SUCCESS
                }
                EFFECT_DOWNLOAD_FAILED -> {
                    SHOW_DOWNLOAD_BUTTON
                }
                EFFECT_APPLY_FAILED -> {
                    FAILED
                }
            }
        return ImageEffectFloatingSheetViewModel(
            myPhotosClickListener = {},
            collapseFloatingSheetListener = {},
            object : EffectSwitchListener {
                override fun onEffectSwitchChanged(
                    effect: EffectEnumInterface,
                    isChecked: Boolean,
                ) {
                    if (previewActionsInteractor.isTargetEffect(effect)) {
                        if (isChecked) {
                            previewActionsInteractor.enableImageEffect(effect)
                        } else {
                            previewActionsInteractor.disableImageEffect()
                        }
                    }
                }
            },
            object : EffectDownloadClickListener {
                override fun onEffectDownloadClick() {
                    if (isWifiOnAndConnected()) {
                        previewActionsInteractor.startEffectsModelDownload(effect)
                    } else {
                        _imageEffectConfirmDownloadDialogViewModel.value =
                            ImageEffectDialogViewModel(
                                onDismiss = {
                                    _imageEffectConfirmDownloadDialogViewModel.value = null
                                },
                                onContinue = {
                                    // Continue to download the ML model
                                    previewActionsInteractor.startEffectsModelDownload(effect)
                                },
                            )
                    }
                }
            },
            floatingSheetViewStatus,
            imageEffectsModel.resultCode,
            imageEffectsModel.errorMessage,
            effect.title,
            effect.type,
            previewActionsInteractor.getEffectTextRes(),
        )
    }

    private fun isWifiOnAndConnected(): Boolean {
        val wifiMgr = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return if (wifiMgr.isWifiEnabled) { // Wi-Fi adapter is ON
            val connMgr =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiInfo = wifiMgr.connectionInfo
            val wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            val signalLevel = wifiMgr.calculateSignalLevel(wifiInfo.rssi)
            signalLevel > 0 && wifi!!.isConnectedOrConnecting
        } else {
            false
        }
    }

    val isEffectsVisible: Flow<Boolean> =
        combine(
            imageEffectFloatingSheetViewModel,
            creativeEffectFloatingSheetViewModel,
            wallpaperPreviewInteractor.wallpaperModel,
        ) { imageEffect, creativeEffect, wallpaperModel ->
            isExtendedWallpaperEffectAvailable(wallpaperModel) ||
                imageEffect != null ||
                creativeEffect != null
        }

    private fun isExtendedWallpaperEffectAvailable(model: WallpaperModel?) =
        flags.isExtendedWallpaperEnabled() &&
            model is LiveWallpaperModel &&
            model.liveWallpaperData.isEffectWallpaper &&
            isExtendedEffectWallpaper(
                context,
                model.liveWallpaperData.systemWallpaperInfo.component,
            )

    private val _isEffectsChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEffectsChecked: Flow<Boolean> = _isEffectsChecked.asStateFlow()

    private val extendedWallpaperIntent =
        Intent().apply {
            component =
                ComponentName(extendedWallpaperEffectPkgName, extendedWallpaperEffectActivityName)
        }

    private val isExtendedEffectAvailable: Flow<Boolean> =
        wallpaperPreviewInteractor.wallpaperModel.map {
            flags.isExtendedWallpaperEnabled() &&
                ((it is StaticWallpaperModel && it.imageWallpaperData?.uri != null) ||
                    (it is LiveWallpaperModel && it.liveWallpaperData.isEffectWallpaper)) &&
                extendedWallpaperIntent.resolveActivityInfo(context.packageManager, 0) != null
        }

    val onEffectsClicked: Flow<((ActivityResultLauncher<Intent>) -> Unit)?> =
        combine(isEffectsVisible, isEffectsChecked, isExtendedEffectAvailable) {
            isVisible,
            isChecked,
            extendedEffectAvailable ->
            if (isVisible) {
                if (extendedEffectAvailable) {
                    { launcher -> launchExtendedWallpaperEffects(launcher) }
                } else {
                    fun(_: ActivityResultLauncher<Intent>) {
                        if (!isChecked) {
                            uncheckAllOthersExcept(EFFECTS)
                        }
                        _isEffectsChecked.value = !isChecked
                    }
                }
            } else {
                null
            }
        }

    private fun launchExtendedWallpaperEffects(launcher: ActivityResultLauncher<Intent>) {
        val wallpaperModel = previewActionsInteractor.wallpaperModel.value
        if (isExtendedEffectWallpaperModel(wallpaperModel)) {
            // Extended effect wallpaper, launch with description
            extendedWallpaperIntent.putExtra(
                WALLPAPER_DESCRIPTION_CONTENT_HANDLING,
                (wallpaperModel as LiveWallpaperModel).liveWallpaperData.description,
            )
        } else {
            val photoUri = (wallpaperModel as? StaticWallpaperModel)?.imageWallpaperData?.uri
            Log.d(TAG, "PhotoURI is: $photoUri")
            photoUri?.let {
                context.grantUriPermission(
                    extendedWallpaperEffectPkgName,
                    photoUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                extendedWallpaperIntent.putExtra("PHOTO_URI", it)
            }
        }

        try {
            launcher.launch(extendedWallpaperIntent)
        } catch (ex: ActivityNotFoundException) {
            Log.e(TAG, "Extended Wallpaper Activity is not available", ex)
        }
    }

    val effectDownloadFailureToastText: Flow<String> =
        previewActionsInteractor.imageEffectsModel
            .map { if (it.status == EFFECT_DOWNLOAD_FAILED) it.errorMessage else null }
            .filterNotNull()

    /** [SHARE] */
    val shareIntent: Flow<Intent?> =
        previewActionsInteractor.wallpaperModel.map { model ->
            (model as? LiveWallpaperModel)?.creativeWallpaperData?.let { data ->
                if (data.shareUri == null || data.shareUri == Uri.EMPTY) null
                else data.getShareIntent()
            }
        }
    val isShareVisible: Flow<Boolean> = shareIntent.map { it != null }

    // Floating sheet contents for the bottom sheet dialog. If content is null, the bottom sheet
    // should collapse, otherwise, expended.
    val previewFloatingSheetViewModel: Flow<PreviewFloatingSheetViewModel?> =
        combine7(
            isInformationChecked,
            isEffectsChecked,
            isCustomizeChecked,
            informationFloatingSheetViewModel,
            imageEffectFloatingSheetViewModel,
            creativeEffectFloatingSheetViewModel,
            customizeFloatingSheetViewModel,
        ) {
            isInformationChecked,
            isEffectsChecked,
            isCustomizeChecked,
            informationFloatingSheetViewModel,
            imageEffectFloatingSheetViewModel,
            creativeEffectFloatingSheetViewModel,
            customizeFloatingSheetViewModel ->
            if (isInformationChecked && informationFloatingSheetViewModel != null) {
                PreviewFloatingSheetViewModel(
                    informationFloatingSheetViewModel = informationFloatingSheetViewModel
                )
            } else if (isEffectsChecked && imageEffectFloatingSheetViewModel != null) {
                PreviewFloatingSheetViewModel(
                    imageEffectFloatingSheetViewModel = imageEffectFloatingSheetViewModel
                )
            } else if (isEffectsChecked && creativeEffectFloatingSheetViewModel != null) {
                PreviewFloatingSheetViewModel(
                    creativeEffectFloatingSheetViewModel = creativeEffectFloatingSheetViewModel
                )
            } else if (isCustomizeChecked && customizeFloatingSheetViewModel != null) {
                PreviewFloatingSheetViewModel(
                    customizeFloatingSheetViewModel = customizeFloatingSheetViewModel
                )
            } else {
                null
            }
        }

    fun onFloatingSheetCollapsed() {
        // When floating collapsed, we should look for those actions that expand the floating sheet
        // and see which is checked, and uncheck it.
        if (_isInformationChecked.value) {
            _isInformationChecked.value = false
        }

        if (_isEffectsChecked.value) {
            _isEffectsChecked.value = false
        }

        if (_isCustomizeChecked.value) {
            _isCustomizeChecked.value = false
        }
    }

    fun isAnyActionChecked(): Boolean =
        _isInformationChecked.value ||
            _isDeleteChecked.value ||
            _isEditChecked.value ||
            _isCustomizeChecked.value ||
            _isEffectsChecked.value

    val isActionChecked: Flow<Boolean> =
        combine(
            isInformationChecked,
            isDeleteChecked,
            isEditChecked,
            isCustomizeChecked,
            isEffectsChecked,
        ) {
            isInformationChecked,
            isDeleteChecked,
            isEditChecked,
            isCustomizeChecked,
            isEffectsChecked ->
            isInformationChecked ||
                isDeleteChecked ||
                isEditChecked ||
                isCustomizeChecked ||
                isEffectsChecked
        }

    private fun uncheckAllOthersExcept(action: Action) {
        if (action != INFORMATION) {
            _isInformationChecked.value = false
        }
        if (action != DELETE) {
            _isDeleteChecked.value = false
        }
        if (action != EDIT) {
            _isEditChecked.value = false
        }
        if (action != CUSTOMIZE) {
            _isCustomizeChecked.value = false
        }
        if (action != EFFECTS) {
            _isEffectsChecked.value = false
        }
    }

    private fun isExtendedEffectWallpaperModel(model: WallpaperModel?): Boolean =
        flags.isExtendedWallpaperEnabled() &&
            model is LiveWallpaperModel &&
            model.liveWallpaperData.isEffectWallpaper

    companion object {
        private const val TAG = "PreviewActionsViewModel"
        const val EXTRA_KEY_IS_CREATE_NEW = "is_create_new"
        const val EXTRA_WALLPAPER_DESCRIPTION = "wp_description"

        private fun WallpaperModel.shouldShowInformationFloatingSheet(): Boolean {
            if (
                this is LiveWallpaperModel &&
                    !liveWallpaperData.systemWallpaperInfo.showMetadataInPreview
            ) {
                // If the live wallpaper's flag of showMetadataInPreview is false, do not show the
                // information floating sheet.
                return false
            }
            val attributions = commonWallpaperData.attributions
            val description = (this as? LiveWallpaperModel)?.liveWallpaperData?.description
            val hasDescription =
                liveWallpaperContentHandling() &&
                    description != null &&
                    (description.description.isNotEmpty() ||
                        !description.title.isNullOrEmpty() ||
                        description.contextUri != null)
            // Show information floating sheet when any of the following contents exists
            // 1. Attributions/Description: Any of the list values is not null nor empty
            // 2. Explore action URL
            return (!attributions.isNullOrEmpty() && attributions.any { it.isNotEmpty() }) ||
                !commonWallpaperData.exploreActionUrl.isNullOrEmpty() ||
                hasDescription
        }

        private fun CreativeWallpaperData.getShareIntent(): Intent {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri)
            shareIntent.setType("image/*")
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.clipData = ClipData.newRawUri(null, shareUri)
            return Intent.createChooser(shareIntent, null)
        }

        private fun LiveWallpaperModel.canBeDeleted(): Boolean {
            return if (creativeWallpaperData != null) {
                !liveWallpaperData.isApplied &&
                    !creativeWallpaperData.isCurrent &&
                    creativeWallpaperData.deleteUri.toString().isNotEmpty()
            } else {
                !liveWallpaperData.isApplied
            }
        }

        /**
         * @param isCreateNew: True means creating a new creative wallpaper. False means editing an
         *   existing wallpaper.
         */
        fun LiveWallpaperData.getEditActivityIntent(isCreateNew: Boolean): Intent? {
            val settingsActivity = systemWallpaperInfo.settingsActivity
            if (settingsActivity.isNullOrEmpty()) {
                return null
            }
            val intent =
                Intent().apply {
                    component = ComponentName(systemWallpaperInfo.packageName, settingsActivity)
                    putExtra(WallpaperSettingsActivity.EXTRA_PREVIEW_MODE, true)
                    putExtra(EXTRA_KEY_IS_CREATE_NEW, isCreateNew)
                    description.content.let { putExtra(EXTRA_WALLPAPER_DESCRIPTION, it) }
                }
            return intent
        }

        fun LiveWallpaperModel.isNewCreativeWallpaper(): Boolean {
            return if (
                InjectorProvider.getInjector().getFlags().isNewCreativeWallpaperCategoryEnabled()
            ) {
                creativeWallpaperData?.isNewCreativeWallpaper ?: false
            } else {
                creativeWallpaperData?.deleteUri?.toString()?.isEmpty() == true
            }
        }

        /** The original combine function can only take up to 5 flows. */
        inline fun <T1, T2, T3, T4, T5, T6, T7, R> combine7(
            flow: Flow<T1>,
            flow2: Flow<T2>,
            flow3: Flow<T3>,
            flow4: Flow<T4>,
            flow5: Flow<T5>,
            flow6: Flow<T6>,
            flow7: Flow<T7>,
            crossinline transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
        ): Flow<R> {
            return combine(flow, flow2, flow3, flow4, flow5, flow6, flow7) { args: Array<*> ->
                @Suppress("UNCHECKED_CAST")
                transform(
                    args[0] as T1,
                    args[1] as T2,
                    args[2] as T3,
                    args[3] as T4,
                    args[4] as T5,
                    args[5] as T6,
                    args[6] as T7,
                )
            }
        }
    }
}

enum class Action {
    INFORMATION,
    DOWNLOAD,
    DELETE,
    EDIT,
    CUSTOMIZE,
    EFFECTS,
    SHARE,
}
