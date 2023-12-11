package com.android.wallpaper.util.wallpaperconnection

import android.app.WallpaperInfo
import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.graphics.Point
import android.os.RemoteException
import android.service.wallpaper.IWallpaperEngine
import android.service.wallpaper.IWallpaperService
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Display
import android.view.SurfaceControl
import android.view.SurfaceView
import android.view.View
import com.android.wallpaper.picker.di.modules.MainDispatcher
import com.android.wallpaper.util.ScreenSizeCalculator
import com.android.wallpaper.util.WallpaperConnection
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WallpaperConnectionUtils {

    private val engineMap = mutableMapOf<String, Deferred<IWallpaperEngine>>()
    private val mutex = Mutex()

    /** Only call this function when the surface view is attached. */
    suspend fun connect(
        context: Context,
        @MainDispatcher mainScope: CoroutineScope,
        wallpaperInfo: WallpaperInfo,
        destinationFlag: Int,
        surfaceView: SurfaceView,
    ) {
        val displayMetrics = getDisplayMetrics(surfaceView)
        val engineKey = wallpaperInfo.getKey()

        if (!engineMap.containsKey(engineKey)) {
            mutex.withLock {
                if (!engineMap.containsKey(engineKey)) {
                    engineMap[engineKey] =
                        mainScope.async {
                            initEngine(
                                context,
                                wallpaperInfo.getWallpaperIntent(),
                                displayMetrics,
                                destinationFlag,
                                surfaceView,
                            )
                        }
                }
            }
        }

        engineMap[engineKey]?.await()?.let { mirrorAndReparent(it, surfaceView, displayMetrics) }
    }

    private suspend fun initEngine(
        context: Context,
        wallpaperIntent: Intent,
        displayMetrics: Point,
        destinationFlag: Int,
        surfaceView: SurfaceView
    ): IWallpaperEngine {
        // Bind service
        val wallpaperService = bindService(context, wallpaperIntent)
        // Attach wallpaper connection to service and get wallpaper engine
        return WallpaperEngineConnection(displayMetrics)
            .getEngine(wallpaperService, destinationFlag, surfaceView)
    }

    private fun WallpaperInfo.getWallpaperIntent(): Intent {
        return Intent(WallpaperService.SERVICE_INTERFACE)
            .setClassName(this.packageName, this.serviceName)
    }

    private fun WallpaperInfo.getKey(): String {
        return this.packageName.plus(":").plus(this.serviceName)
    }

    private suspend fun bindService(context: Context, intent: Intent): IWallpaperService =
        suspendCancellableCoroutine { k: CancellableContinuation<IWallpaperService> ->
            val serviceConnection =
                WallpaperServiceConnection(
                    object : WallpaperServiceConnection.WallpaperServiceConnectionListener {
                        override fun onWallpaperServiceConnected(
                            wallpaperService: IWallpaperService
                        ) {
                            k.resumeWith(Result.success(wallpaperService))
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
            if (!success) {
                k.resumeWith(Result.failure(Exception("Fail to bind the live wallpaper service.")))
            }
        }

    private fun mirrorAndReparent(
        engine: IWallpaperEngine,
        parentSurface: SurfaceView,
        displayMetrics: Point
    ) {
        fun logError(e: Exception) {
            Log.e(WallpaperConnection::class.simpleName, "Fail to reparent wallpaper surface", e)
        }

        try {
            val parentSC = parentSurface.surfaceControl
            val wallpaperMirrorSC = engine.mirrorSurfaceControl() ?: return
            val values = getScale(parentSurface, displayMetrics)
            SurfaceControl.Transaction().use { t ->
                t.setMatrix(
                    wallpaperMirrorSC,
                    values[Matrix.MSCALE_X],
                    values[Matrix.MSKEW_Y],
                    values[Matrix.MSKEW_X],
                    values[Matrix.MSCALE_Y]
                )
                t.reparent(wallpaperMirrorSC, parentSC)
                t.show(wallpaperMirrorSC)
                t.apply()
            }
        } catch (e: RemoteException) {
            logError(e)
        } catch (e: NullPointerException) {
            logError(e)
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

    private fun getDisplayMetrics(view: View): Point {
        val screenSizeCalculator = ScreenSizeCalculator.getInstance()
        val display: Display = view.display
        return screenSizeCalculator.getScreenSize(display)
    }
}
