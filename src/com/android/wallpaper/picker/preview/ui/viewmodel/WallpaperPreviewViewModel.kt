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

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.stats.style.StyleEnums
import android.view.accessibility.AccessibilityManager
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.BasePreviewActivity.EXTRA_VIEW_AS_HOME
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.picker.di.modules.HomeScreenPreviewUtils
import com.android.wallpaper.picker.di.modules.LockScreenPreviewUtils
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository
import com.android.wallpaper.picker.preview.domain.interactor.PreviewActionsInteractor
import com.android.wallpaper.picker.preview.domain.interactor.WallpaperPreviewInteractor
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import com.android.wallpaper.picker.preview.ui.binder.PreviewTooltipBinder
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.WallpaperConnection.WhichPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.EnumSet
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** Top level [ViewModel] for [WallpaperPreviewActivity] and its fragments */
@HiltViewModel
class WallpaperPreviewViewModel
@Inject
constructor(
    private val interactor: WallpaperPreviewInteractor,
    actionsInteractor: PreviewActionsInteractor,
    staticWallpaperPreviewViewModelFactory: StaticWallpaperPreviewViewModel.Factory,
    val previewActionsViewModel: PreviewActionsViewModel,
    private val displayUtils: DisplayUtils,
    @HomeScreenPreviewUtils private val homePreviewUtils: PreviewUtils,
    @LockScreenPreviewUtils private val lockPreviewUtils: PreviewUtils,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Don't update smaller display since we always use portrait, always use wallpaper display on
    // single display device.
    val smallerDisplaySize = displayUtils.getRealSize(displayUtils.getSmallerDisplay())
    private val _wallpaperDisplaySize =
        MutableStateFlow(displayUtils.getRealSize(displayUtils.getWallpaperDisplay()))
    val wallpaperDisplaySize = _wallpaperDisplaySize.asStateFlow()

    val staticWallpaperPreviewViewModel =
        staticWallpaperPreviewViewModelFactory.create(viewModelScope)

    var isNewTask = false

    val isViewAsHome = savedStateHandle.get<Boolean>(EXTRA_VIEW_AS_HOME) ?: false

    fun getWallpaperPreviewSource(): Screen =
        if (isViewAsHome) Screen.HOME_SCREEN else Screen.LOCK_SCREEN

    val wallpaper: StateFlow<WallpaperModel?> = interactor.wallpaperModel

    fun setPreviewWallpaperModel(wallpaperModel: WallpaperModel) {
        interactor.setPreviewWallpaper(wallpaperModel)
    }

    // Used to display loading indication on the preview.
    val imageEffectsModel = actionsInteractor.imageEffectsModel

    // This flag prevents launching the creative edit activity again when orientation change.
    // On orientation change, the fragment's onCreateView will be called again.
    var isCurrentlyEditingCreativeWallpaper = false

    private val _currentPreviewScreen = MutableStateFlow(PreviewScreen.SMALL_PREVIEW)
    val currentPreviewScreen = _currentPreviewScreen.asStateFlow()

    val shouldEnableClickOnPager: Flow<Boolean> =
        _currentPreviewScreen.map { it != PreviewScreen.FULL_PREVIEW }

    val smallPreviewTabs = Screen.entries.toList()

    private val _smallPreviewSelectedTab = MutableStateFlow(getWallpaperPreviewSource())
    val smallPreviewSelectedTab = _smallPreviewSelectedTab.asStateFlow()

    val smallPreviewSelectedTabIndex = smallPreviewSelectedTab.map { smallPreviewTabs.indexOf(it) }

    /**
     * Returns true if back pressed is handled due to conditions like users at a secondary screen.
     */
    fun handleBackPressed(): Boolean {
        if (_currentPreviewScreen.value == PreviewScreen.APPLY_WALLPAPER) {
            _currentPreviewScreen.value = PreviewScreen.SMALL_PREVIEW
            return true
        } else if (_currentPreviewScreen.value == PreviewScreen.FULL_PREVIEW) {
            _currentPreviewScreen.value = PreviewScreen.SMALL_PREVIEW
            // TODO(b/367374790): Returns true when shared element transition is removed
            return false
        }
        return false
    }

    fun getSmallPreviewTabIndex(): Int {
        return smallPreviewTabs.indexOf(smallPreviewSelectedTab.value)
    }

    fun setSmallPreviewSelectedTab(screen: Screen) {
        _smallPreviewSelectedTab.value = screen
    }

    fun setSmallPreviewSelectedTabIndex(index: Int) {
        _smallPreviewSelectedTab.value = smallPreviewTabs[index]
    }

    fun updateDisplayConfiguration() {
        _wallpaperDisplaySize.value = displayUtils.getRealSize(displayUtils.getWallpaperDisplay())
    }

    private val isWallpaperCroppable: Flow<Boolean> =
        wallpaper.map { wallpaper ->
            wallpaper is StaticWallpaperModel && !wallpaper.isDownloadableWallpaper()
        }

    val smallTooltipViewModel =
        object : PreviewTooltipBinder.TooltipViewModel {
            override val shouldShowTooltip: Flow<Boolean> =
                combine(isWallpaperCroppable, interactor.hasSmallPreviewTooltipBeenShown) {
                        isCroppable,
                        hasTooltipBeenShown ->
                        // Only show tooltip if it has not been shown before.
                        isCroppable && !hasTooltipBeenShown
                    }
                    .distinctUntilChanged()

            override fun dismissTooltip() = interactor.hideSmallPreviewTooltip()
        }

    val fullTooltipViewModel =
        object : PreviewTooltipBinder.TooltipViewModel {
            override val shouldShowTooltip: Flow<Boolean> =
                combine(isWallpaperCroppable, interactor.hasFullPreviewTooltipBeenShown) {
                        isCroppable,
                        hasTooltipBeenShown ->
                        // Only show tooltip if it has not been shown before.
                        isCroppable && !hasTooltipBeenShown
                    }
                    .distinctUntilChanged()

            override fun dismissTooltip() = interactor.hideFullPreviewTooltip()
        }

    private val _whichPreview = MutableStateFlow<WhichPreview?>(null)
    private val whichPreview: Flow<WhichPreview> = _whichPreview.asStateFlow().filterNotNull()

    fun setWhichPreview(whichPreview: WhichPreview) {
        _whichPreview.value = whichPreview
    }

    fun setCropHints(cropHints: Map<Point, Rect>) {
        wallpaper.value?.let { model ->
            if (model is StaticWallpaperModel && !model.isDownloadableWallpaper()) {
                staticWallpaperPreviewViewModel.updateCropHintsInfo(
                    cropHints.mapValues {
                        FullPreviewCropModel(cropHint = it.value, cropSizeModel = null)
                    }
                )
            }
        }
    }

    private val _isWallpaperColorPreviewEnabled = MutableStateFlow(false)
    val isWallpaperColorPreviewEnabled = _isWallpaperColorPreviewEnabled.asStateFlow()

    fun setIsWallpaperColorPreviewEnabled(isWallpaperColorPreviewEnabled: Boolean) {
        _isWallpaperColorPreviewEnabled.value = isWallpaperColorPreviewEnabled
    }

    private val _wallpaperConnectionColors: MutableStateFlow<WallpaperColorsModel> =
        MutableStateFlow(WallpaperColorsModel.Loading as WallpaperColorsModel).apply {
            viewModelScope.launch {
                delay(1000)
                if (value == WallpaperColorsModel.Loading) {
                    emit(WallpaperColorsModel.Loaded(null))
                }
            }
        }
    private val liveWallpaperColors: Flow<WallpaperColorsModel> =
        wallpaper
            .filter { it is LiveWallpaperModel }
            .combine(_wallpaperConnectionColors) { _, wallpaperConnectionColors ->
                wallpaperConnectionColors
            }
    val wallpaperColorsModel: Flow<WallpaperColorsModel> =
        merge(liveWallpaperColors, staticWallpaperPreviewViewModel.wallpaperColors).combine(
            isWallpaperColorPreviewEnabled
        ) { colors, isEnabled ->
            if (isEnabled) colors else WallpaperColorsModel.Loaded(null)
        }

    // This is only used for the full screen preview.
    private val _fullPreviewConfigViewModel: MutableStateFlow<FullPreviewConfigViewModel?> =
        MutableStateFlow(null)
    val fullPreviewConfigViewModel = _fullPreviewConfigViewModel.asStateFlow()

    // This is only used for the small screen wallpaper preview.
    val smallWallpaper: Flow<Pair<WallpaperModel, WhichPreview>> =
        combine(wallpaper.filterNotNull(), whichPreview) { wallpaper, whichPreview ->
            Pair(wallpaper, whichPreview)
        }

    // This is only used for the full screen wallpaper preview.
    val fullWallpaper: Flow<FullWallpaperPreviewViewModel> =
        combine(
            wallpaper.filterNotNull(),
            fullPreviewConfigViewModel.filterNotNull(),
            whichPreview,
            wallpaperDisplaySize,
        ) { wallpaper, config, whichPreview, wallpaperDisplaySize ->
            val displaySize =
                when (config.deviceDisplayType) {
                    DeviceDisplayType.SINGLE -> wallpaperDisplaySize
                    DeviceDisplayType.FOLDED -> smallerDisplaySize
                    DeviceDisplayType.UNFOLDED -> wallpaperDisplaySize
                }
            FullWallpaperPreviewViewModel(
                wallpaper = wallpaper,
                config = FullPreviewConfigViewModel(config.screen, config.deviceDisplayType),
                displaySize = displaySize,
                allowUserCropping =
                    wallpaper is StaticWallpaperModel && !wallpaper.isDownloadableWallpaper(),
                whichPreview = whichPreview,
            )
        }

    // This is only used for the full screen workspace preview.
    val fullWorkspacePreviewConfigViewModel: Flow<WorkspacePreviewConfigViewModel> =
        fullPreviewConfigViewModel.filterNotNull().map {
            getWorkspacePreviewConfig(it.screen, it.deviceDisplayType)
        }

    val onCropButtonClick: Flow<(() -> Unit)?> =
        combine(wallpaper, fullPreviewConfigViewModel.filterNotNull(), fullWallpaper) {
            wallpaper,
            _,
            fullWallpaper ->
            if (wallpaper is StaticWallpaperModel && !wallpaper.isDownloadableWallpaper()) {
                {
                    staticWallpaperPreviewViewModel.run {
                        updateCropHintsInfo(
                            fullPreviewCropModels.filterKeys { it == fullWallpaper.displaySize }
                        )
                    }
                }
            } else {
                null
            }
        }

    // Set wallpaper button and set wallpaper dialog
    val isSetWallpaperButtonVisible: Flow<Boolean> =
        wallpaper.map { it != null && !it.isDownloadableWallpaper() }

    val isSetWallpaperButtonEnabled: Flow<Boolean> =
        combine(
            isSetWallpaperButtonVisible,
            wallpaper,
            staticWallpaperPreviewViewModel.fullResWallpaperViewModel,
            actionsInteractor.imageEffectsModel,
        ) { isSetWallpaperButtonVisible, wallpaper, fullResWallpaperViewModel, imageEffectsModel ->
            isSetWallpaperButtonVisible &&
                !(wallpaper is StaticWallpaperModel && fullResWallpaperViewModel == null) &&
                imageEffectsModel.status !=
                    ImageEffectsRepository.EffectStatus.EFFECT_APPLY_IN_PROGRESS
        }

    val onSetWallpaperButtonClicked: Flow<(() -> Unit)?> =
        combine(isSetWallpaperButtonVisible, isSetWallpaperButtonEnabled) {
            isSetWallpaperButtonVisible,
            isSetWallpaperButtonEnabled ->
            if (isSetWallpaperButtonVisible && isSetWallpaperButtonEnabled) {
                { _showSetWallpaperDialog.value = true }
            } else null
        }

    val onNextButtonClicked: Flow<(() -> Unit)?> =
        isSetWallpaperButtonEnabled.map {
            if (it) {
                { _currentPreviewScreen.value = PreviewScreen.APPLY_WALLPAPER }
            } else null
        }

    val onCancelButtonClicked: Flow<() -> Unit> = flowOf {
        _currentPreviewScreen.value = PreviewScreen.SMALL_PREVIEW
    }

    private val _showSetWallpaperDialog = MutableStateFlow(false)
    val showSetWallpaperDialog = _showSetWallpaperDialog.asStateFlow()

    private val _setWallpaperDialogSelectedScreens: MutableStateFlow<Set<Screen>> =
        MutableStateFlow(EnumSet.allOf(Screen::class.java))
    val setWallpaperDialogSelectedScreens: StateFlow<Set<Screen>> =
        _setWallpaperDialogSelectedScreens.asStateFlow()

    val isApplyButtonEnabled: Flow<Boolean> =
        setWallpaperDialogSelectedScreens.map { it.isNotEmpty() }

    val isHomeCheckBoxChecked: Flow<Boolean> =
        setWallpaperDialogSelectedScreens.map { it.contains(Screen.HOME_SCREEN) }

    val isLockCheckBoxChecked: Flow<Boolean> =
        setWallpaperDialogSelectedScreens.map { it.contains(Screen.LOCK_SCREEN) }

    val onHomeCheckBoxChecked: Flow<() -> Unit> = flowOf {
        onSetWallpaperDialogScreenSelected(Screen.HOME_SCREEN)
    }

    val onLockCheckBoxChecked: Flow<() -> Unit> = flowOf {
        onSetWallpaperDialogScreenSelected(Screen.LOCK_SCREEN)
    }

    fun onSetWallpaperDialogScreenSelected(screen: Screen) {
        val previousSelection = _setWallpaperDialogSelectedScreens.value
        _setWallpaperDialogSelectedScreens.value =
            if (
                previousSelection.contains(screen) &&
                    (previousSelection.size > 1 || BaseFlags.get().isNewPickerUi())
            ) {
                previousSelection.minus(screen)
            } else {
                previousSelection.plus(screen)
            }
    }

    private val _isSetWallpaperProgressBarVisible = MutableStateFlow(false)
    val isSetWallpaperProgressBarVisible: Flow<Boolean> =
        _isSetWallpaperProgressBarVisible.asStateFlow()

    val setWallpaperDialogOnConfirmButtonClicked: Flow<suspend () -> Unit> =
        combine(
            wallpaper.filterNotNull(),
            staticWallpaperPreviewViewModel.fullResWallpaperViewModel,
            setWallpaperDialogSelectedScreens,
        ) { wallpaper, fullResWallpaperViewModel, selectedScreens ->
            {
                _isSetWallpaperProgressBarVisible.value = true
                val destination = selectedScreens.getDestination()
                _showSetWallpaperDialog.value = false
                when (wallpaper) {
                    is StaticWallpaperModel ->
                        fullResWallpaperViewModel?.let {
                            interactor.setStaticWallpaper(
                                setWallpaperEntryPoint =
                                    StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW,
                                destination = destination,
                                wallpaperModel = wallpaper,
                                bitmap = it.rawWallpaperBitmap,
                                wallpaperSize = it.rawWallpaperSize,
                                asset = it.asset,
                                fullPreviewCropModels =
                                    if (it.fullPreviewCropModels.isNullOrEmpty()) {
                                        staticWallpaperPreviewViewModel.fullPreviewCropModels
                                    } else {
                                        it.fullPreviewCropModels
                                    },
                            )
                        }
                    is LiveWallpaperModel -> {
                        interactor.setLiveWallpaper(
                            setWallpaperEntryPoint =
                                StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW,
                            destination = destination,
                            wallpaperModel = wallpaper,
                        )
                    }
                }
            }
        }

    private fun Set<Screen>.getDestination(): WallpaperDestination {
        return if (containsAll(Screen.entries)) {
            WallpaperDestination.BOTH
        } else if (contains(Screen.HOME_SCREEN)) {
            WallpaperDestination.HOME
        } else if (contains(Screen.LOCK_SCREEN)) {
            WallpaperDestination.LOCK
        } else {
            throw IllegalArgumentException("Unknown screens selected: $this")
        }
    }

    fun dismissSetWallpaperDialog() {
        _showSetWallpaperDialog.value = false
    }

    fun setWallpaperConnectionColors(wallpaperColors: WallpaperColorsModel) {
        _wallpaperConnectionColors.value = wallpaperColors
    }

    fun getWorkspacePreviewConfig(
        screen: Screen,
        deviceDisplayType: DeviceDisplayType,
    ): WorkspacePreviewConfigViewModel {
        val previewUtils =
            when (screen) {
                Screen.HOME_SCREEN -> {
                    homePreviewUtils
                }
                Screen.LOCK_SCREEN -> {
                    lockPreviewUtils
                }
            }
        // Do not directly store display Id in the view model because display Id can change on fold
        // and unfold whereas view models persist. Store FoldableDisplay instead and convert in the
        // binder.
        return WorkspacePreviewConfigViewModel(
            previewUtils = previewUtils,
            deviceDisplayType = deviceDisplayType,
        )
    }

    fun getDisplayId(deviceDisplayType: DeviceDisplayType): Int {
        return when (deviceDisplayType) {
            DeviceDisplayType.SINGLE -> {
                displayUtils.getWallpaperDisplay().displayId
            }
            DeviceDisplayType.FOLDED -> {
                displayUtils.getSmallerDisplay().displayId
            }
            DeviceDisplayType.UNFOLDED -> {
                displayUtils.getWallpaperDisplay().displayId
            }
        }
    }

    val isSmallPreviewClickable =
        actionsInteractor.imageEffectsModel.map {
            it.status != ImageEffectsRepository.EffectStatus.EFFECT_APPLY_IN_PROGRESS
        }

    fun onSmallPreviewClicked(
        screen: Screen,
        deviceDisplayType: DeviceDisplayType,
        navigate: () -> Unit,
    ): Flow<(() -> Unit)?> =
        combine(isSmallPreviewClickable, smallPreviewSelectedTab) { isClickable, selectedTab ->
            if (isClickable) {
                if (selectedTab == screen) {
                    // If the selected preview matches the selected tab, navigate to full preview.
                    {
                        smallTooltipViewModel.dismissTooltip()
                        _fullPreviewConfigViewModel.value =
                            FullPreviewConfigViewModel(screen, deviceDisplayType)
                        navigate()
                    }
                } else {
                    // If the selected preview doesn't match the selected tab, switch tab to match.
                    { setSmallPreviewSelectedTab(screen) }
                }
            } else {
                null
            }
        }

    fun setDefaultFullPreviewConfigViewModel(deviceDisplayType: DeviceDisplayType) {
        _fullPreviewConfigViewModel.value =
            FullPreviewConfigViewModel(Screen.HOME_SCREEN, deviceDisplayType)
    }

    fun resetFullPreviewConfigViewModel() {
        _fullPreviewConfigViewModel.value = null
    }

    fun isAccessibilityEnabled(): Boolean {
        return isAccessibilityEnabled(
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        )
    }

    @VisibleForTesting
    fun isAccessibilityEnabled(am: AccessibilityManager): Boolean {
        val enabledServices =
            am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_AUDIBLE or
                    AccessibilityServiceInfo.FEEDBACK_SPOKEN or
                    AccessibilityServiceInfo.FEEDBACK_VISUAL or
                    AccessibilityServiceInfo.FEEDBACK_HAPTIC or
                    AccessibilityServiceInfo.FEEDBACK_BRAILLE
            )
        return enabledServices.isNotEmpty()
    }

    companion object {
        private fun WallpaperModel.isDownloadableWallpaper(): Boolean {
            return this is StaticWallpaperModel && downloadableWallpaperData != null
        }

        /** The current preview screen or the screen being transition to. */
        enum class PreviewScreen {
            SMALL_PREVIEW,
            FULL_PREVIEW,
            APPLY_WALLPAPER,
        }
    }
}
