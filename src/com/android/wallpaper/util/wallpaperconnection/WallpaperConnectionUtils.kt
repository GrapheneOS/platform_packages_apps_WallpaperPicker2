package com.android.wallpaper.util.wallpaperconnection

import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Matrix
import android.graphics.Point
import android.net.Uri
import android.os.IBinder
import android.os.RemoteException
import android.service.wallpaper.IWallpaperEngine
import android.service.wallpaper.IWallpaperService
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceControl
import android.view.SurfaceView
import com.android.app.tracing.TraceUtils.traceAsync
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.util.WallpaperConnection
import com.android.wallpaper.util.WallpaperConnection.WhichPreview
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WallpaperConnectionUtils {

    const val TAG = "WallpaperConnectionUtils"

    // engineMap and surfaceControlMap are used for disconnecting wallpaper services.
    private val wallpaperConnectionMap = ConcurrentHashMap<String, Deferred<WallpaperConnection>>()
    // Note that when one wallpaper engine's render is mirrored to a new surface view, we call
    // engine.mirrorSurfaceControl() and will have a new surface control instance.
    private val surfaceControlMap = mutableMapOf<String, MutableList<SurfaceControl>>()
    // Track the currently used creative wallpaper config preview URI to avoid unnecessary multiple
    // update queries for the same preview.
    private val creativeWallpaperConfigPreviewUriMap = mutableMapOf<String, Uri>()

    private val mutex = Mutex()

    /** Only call this function when the surface view is attached. */
    suspend fun connect(
        context: Context,
        wallpaperModel: LiveWallpaperModel,
        whichPreview: WhichPreview,
        destinationFlag: Int,
        surfaceView: SurfaceView,
        engineRenderingConfig: EngineRenderingConfig,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
        listener: WallpaperEngineConnection.WallpaperEngineConnectionListener? = null,
    ) {
        val wallpaperInfo = wallpaperModel.liveWallpaperData.systemWallpaperInfo
        val engineDisplaySize = engineRenderingConfig.getEngineDisplaySize()
        val engineKey = wallpaperInfo.getKey(engineDisplaySize)

        traceAsync(TAG, "connect") {
            // Update the creative wallpaper uri before starting the service.
            wallpaperModel.creativeWallpaperData?.configPreviewUri?.let {
                val uriKey = wallpaperInfo.getKey()
                if (!creativeWallpaperConfigPreviewUriMap.containsKey(uriKey)) {
                    mutex.withLock {
                        if (!creativeWallpaperConfigPreviewUriMap.containsKey(uriKey)) {
                            // First time binding wallpaper should initialize wallpaper preview.
                            if (isFirstBindingDeferred.await()) {
                                context.contentResolver.update(it, ContentValues(), null)
                            }
                            creativeWallpaperConfigPreviewUriMap[uriKey] = it
                        }
                    }
                }
            }

            if (!wallpaperConnectionMap.containsKey(engineKey)) {
                mutex.withLock {
                    if (!wallpaperConnectionMap.containsKey(engineKey)) {
                        wallpaperConnectionMap[engineKey] = coroutineScope {
                            async {
                                initEngine(
                                    context,
                                    wallpaperModel.getWallpaperServiceIntent(),
                                    engineDisplaySize,
                                    destinationFlag,
                                    whichPreview,
                                    surfaceView,
                                    listener,
                                )
                            }
                        }
                    }
                }
            }

            wallpaperConnectionMap[engineKey]?.await()?.let { (engineConnection, _, _, _) ->
                engineConnection.get()?.engine?.let {
                    mirrorAndReparent(
                        engineKey,
                        it,
                        surfaceView,
                        engineRenderingConfig.getEngineDisplaySize(),
                        engineRenderingConfig.enforceSingleEngine,
                    )
                }
            }
        }
    }

    suspend fun disconnectAll(context: Context) {
        disconnectAllServices(context)
        surfaceControlMap.keys.map { key ->
            mutex.withLock {
                surfaceControlMap[key]?.let { surfaceControls ->
                    surfaceControls.forEach { it.release() }
                    surfaceControls.clear()
                }
            }
        }
        surfaceControlMap.clear()
    }

    /**
     * Disconnect all live wallpaper services without releasing and clear surface controls. This
     * function is called before binding static wallpapers. We have cases that user switch between
     * live wan static wallpapers. When switching from live to static wallpapers, we need to
     * disconnect the live wallpaper services to have the static wallpapers show up. But we can not
     * clear the surface controls yet, because we will need them to render the live wallpapers again
     * when switching from static to live wallpapers again.
     */
    suspend fun disconnectAllServices(context: Context) {
        wallpaperConnectionMap.keys.map { key ->
            mutex.withLock { wallpaperConnectionMap.remove(key)?.await()?.disconnect(context) }
        }

        creativeWallpaperConfigPreviewUriMap.clear()
    }

    suspend fun dispatchTouchEvent(
        wallpaperModel: LiveWallpaperModel,
        engineRenderingConfig: EngineRenderingConfig,
        event: MotionEvent,
    ) {
        val engine =
            wallpaperModel.liveWallpaperData.systemWallpaperInfo
                .getKey(engineRenderingConfig.getEngineDisplaySize())
                .let { engineKey ->
                    wallpaperConnectionMap[engineKey]?.await()?.engineConnection?.get()?.engine
                }

        if (engine != null) {
            val action: Int = event.actionMasked
            val dup = MotionEvent.obtainNoHistory(event).also { it.setLocation(event.x, event.y) }
            val pointerIndex = event.actionIndex
            try {
                engine.dispatchPointer(dup)
                if (action == MotionEvent.ACTION_UP) {
                    engine.dispatchWallpaperCommand(
                        WallpaperManager.COMMAND_TAP,
                        event.x.toInt(),
                        event.y.toInt(),
                        0,
                        null
                    )
                } else if (action == MotionEvent.ACTION_POINTER_UP) {
                    engine.dispatchWallpaperCommand(
                        WallpaperManager.COMMAND_SECONDARY_TAP,
                        event.getX(pointerIndex).toInt(),
                        event.getY(pointerIndex).toInt(),
                        0,
                        null
                    )
                }
            } catch (e: RemoteException) {
                Log.e(TAG, "Remote exception of wallpaper connection", e)
            }
        }
    }

    private fun LiveWallpaperModel.getWallpaperServiceIntent(): Intent {
        return liveWallpaperData.systemWallpaperInfo.let {
            Intent(WallpaperService.SERVICE_INTERFACE).setClassName(it.packageName, it.serviceName)
        }
    }

    private suspend fun initEngine(
        context: Context,
        wallpaperIntent: Intent,
        displayMetrics: Point,
        destinationFlag: Int,
        whichPreview: WhichPreview,
        surfaceView: SurfaceView,
        listener: WallpaperEngineConnection.WallpaperEngineConnectionListener?,
    ): WallpaperConnection {
        // Bind service and get service connection and wallpaper service
        val (serviceConnection, wallpaperService) = bindWallpaperService(context, wallpaperIntent)
        val engineConnection = WallpaperEngineConnection(displayMetrics, whichPreview)
        listener?.let { engineConnection.setListener(it) }
        // Attach wallpaper connection to service and get wallpaper engine
        engineConnection.getEngine(wallpaperService, destinationFlag, surfaceView)
        return WallpaperConnection(
            WeakReference(engineConnection),
            WeakReference(serviceConnection),
            WeakReference(wallpaperService),
            WeakReference(surfaceView.windowToken),
        )
    }

    private fun WallpaperInfo.getKey(displaySize: Point? = null): String {
        val keyWithoutSizeInformation = this.packageName.plus(":").plus(this.serviceName)
        return if (displaySize != null) {
            keyWithoutSizeInformation.plus(":").plus("${displaySize.x}x${displaySize.y}")
        } else {
            keyWithoutSizeInformation
        }
    }

    private suspend fun bindWallpaperService(
        context: Context,
        intent: Intent
    ): Pair<ServiceConnection, IWallpaperService> =
        suspendCancellableCoroutine {
            k: CancellableContinuation<Pair<ServiceConnection, IWallpaperService>> ->
            val serviceConnection =
                WallpaperServiceConnection(
                    object : WallpaperServiceConnection.WallpaperServiceConnectionListener {
                        override fun onWallpaperServiceConnected(
                            serviceConnection: ServiceConnection,
                            wallpaperService: IWallpaperService
                        ) {
                            if (k.isActive) {
                                k.resumeWith(
                                    Result.success(Pair(serviceConnection, wallpaperService))
                                )
                            }
                        }
                    }
                )
            val success =
                context.bindService(
                    intent,
                    serviceConnection,
                    Context.BIND_AUTO_CREATE or
                        Context.BIND_IMPORTANT or
                        Context.BIND_ALLOW_ACTIVITY_STARTS
                )
            if (!success && k.isActive) {
                k.resumeWith(Result.failure(Exception("Fail to bind the live wallpaper service.")))
            }
        }

    private suspend fun mirrorAndReparent(
        engineKey: String,
        engine: IWallpaperEngine,
        parentSurface: SurfaceView,
        displayMetrics: Point,
        enforceSingleEngine: Boolean,
    ) {
        fun logError(e: Exception) {
            Log.e(WallpaperConnection::class.simpleName, "Fail to reparent wallpaper surface", e)
        }

        try {
            val parentSurfaceControl = parentSurface.surfaceControl
            val wallpaperSurfaceControl = engine.mirrorSurfaceControl() ?: return
            // Add surface control reference for later release when disconnected
            addSurfaceControlReference(engineKey, wallpaperSurfaceControl)

            val values = getScale(parentSurface, displayMetrics)
            SurfaceControl.Transaction().use { t ->
                t.setMatrix(
                    wallpaperSurfaceControl,
                    if (enforceSingleEngine) values[Matrix.MSCALE_Y] else values[Matrix.MSCALE_X],
                    values[Matrix.MSKEW_X],
                    values[Matrix.MSKEW_Y],
                    values[Matrix.MSCALE_Y],
                )
                t.reparent(wallpaperSurfaceControl, parentSurfaceControl)
                t.show(wallpaperSurfaceControl)
                t.apply()
            }
        } catch (e: RemoteException) {
            logError(e)
        } catch (e: NullPointerException) {
            logError(e)
        }
    }

    private suspend fun addSurfaceControlReference(
        engineKey: String,
        wallpaperSurfaceControl: SurfaceControl,
    ) {
        val surfaceControls = surfaceControlMap[engineKey]
        if (surfaceControls == null) {
            mutex.withLock {
                surfaceControlMap[engineKey] =
                    (surfaceControlMap[engineKey] ?: mutableListOf()).apply {
                        add(wallpaperSurfaceControl)
                    }
            }
        } else {
            surfaceControls.add(wallpaperSurfaceControl)
        }
    }

    private fun getScale(parentSurface: SurfaceView, displayMetrics: Point): FloatArray {
        val metrics = Matrix()
        val values = FloatArray(9)
        val surfacePosition = parentSurface.holder.surfaceFrame
        metrics.postScale(
            surfacePosition.width().toFloat() / displayMetrics.x,
            surfacePosition.height().toFloat() / displayMetrics.y
        )
        metrics.getValues(values)
        return values
    }

    data class WallpaperConnection(
        val engineConnection: WeakReference<WallpaperEngineConnection>,
        val serviceConnection: WeakReference<ServiceConnection>,
        val wallpaperService: WeakReference<IWallpaperService>,
        val windowToken: WeakReference<IBinder>,
    ) {
        fun disconnect(context: Context) {
            engineConnection.get()?.apply {
                engine?.destroy()
                removeListener()
                engine = null
            }
            windowToken.get()?.let { wallpaperService.get()?.detach(it) }
            serviceConnection.get()?.let { context.unbindService(it) }
        }
    }

    data class EngineRenderingConfig(
        val enforceSingleEngine: Boolean,
        val deviceDisplayType: DeviceDisplayType,
        val smallDisplaySize: Point,
        val wallpaperDisplaySize: Point,
    ) {
        fun getEngineDisplaySize(): Point {
            // If we need to enforce single engine, always return the larger screen's preview
            return if (enforceSingleEngine) {
                return wallpaperDisplaySize
            } else {
                getPreviewDisplaySize()
            }
        }

        private fun getPreviewDisplaySize(): Point {
            return when (deviceDisplayType) {
                DeviceDisplayType.SINGLE -> wallpaperDisplaySize
                DeviceDisplayType.FOLDED -> smallDisplaySize
                DeviceDisplayType.UNFOLDED -> wallpaperDisplaySize
            }
        }
    }

    fun LiveWallpaperModel.shouldEnforceSingleEngine(): Boolean {
        return when {
            creativeWallpaperData != null -> false
            liveWallpaperData.isEffectWallpaper -> false
            else -> true // Only fallback to single engine rendering for legacy live wallpapers
        }
    }
}
