package com.android.wallpaper.util.wallpaperconnection

import android.app.WallpaperColors
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.service.wallpaper.IWallpaperConnection
import android.service.wallpaper.IWallpaperEngine
import android.service.wallpaper.IWallpaperService
import android.view.SurfaceView
import android.view.WindowManager
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

class WallpaperEngineConnection(
    private val displayMetrics: Point,
) : IWallpaperConnection.Stub() {

    var engine: IWallpaperEngine? = null
    private var engineContinuation: CancellableContinuation<IWallpaperEngine>? = null

    suspend fun getEngine(
        wallpaperService: IWallpaperService,
        destinationFlag: Int,
        surfaceView: SurfaceView,
    ): IWallpaperEngine {
        return engine
            ?: suspendCancellableCoroutine { k: CancellableContinuation<IWallpaperEngine> ->
                engineContinuation = k
                attachEngineConnection(
                    wallpaperEngineConnection = this,
                    wallpaperService = wallpaperService,
                    destinationFlag = destinationFlag,
                    surfaceView = surfaceView,
                )
            }
    }

    override fun attachEngine(engine: IWallpaperEngine?, displayId: Int) {
        engine?.apply {
            resizePreview(Rect(0, 0, displayMetrics.x, displayMetrics.y))
            requestWallpaperColors()
        }
    }

    override fun engineShown(engine: IWallpaperEngine?) {
        engine?.let { engineContinuation?.resumeWith(Result.success(it)) }
        this.engine = engine
    }

    override fun onLocalWallpaperColorsChanged(
        area: RectF?,
        colors: WallpaperColors?,
        displayId: Int
    ) {
        // Do nothing intended.
    }

    override fun onWallpaperColorsChanged(p0: WallpaperColors?, p1: Int) {
        // Do nothing intended.
    }

    override fun setWallpaper(p0: String?): ParcelFileDescriptor {
        TODO("Not yet implemented")
    }

    companion object {
        /**
         * Before Android U, [IWallpaperService.attach] has no input of destinationFlag. We do
         * method reflection to probe if the service from the external app is using a pre-U API;
         * otherwise, we use the new one.
         */
        private fun attachEngineConnection(
            wallpaperEngineConnection: WallpaperEngineConnection,
            wallpaperService: IWallpaperService,
            destinationFlag: Int,
            surfaceView: SurfaceView,
        ) {
            try {
                val preUMethod: Method =
                    wallpaperService.javaClass.getMethod(
                        "attach",
                        IWallpaperConnection::class.java,
                        IBinder::class.java,
                        Int::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Rect::class.java,
                        Int::class.javaPrimitiveType
                    )
                preUMethod.invoke(
                    wallpaperService,
                    wallpaperEngineConnection,
                    surfaceView.windowToken,
                    WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA,
                    true,
                    surfaceView.width,
                    surfaceView.height,
                    Rect(0, 0, 0, 0),
                    surfaceView.display.displayId
                )
            } catch (e: Exception) {
                when (e) {
                    is NoSuchMethodException,
                    is InvocationTargetException,
                    is IllegalAccessException ->
                        wallpaperService.attach(
                            wallpaperEngineConnection,
                            surfaceView.windowToken,
                            WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA,
                            true,
                            surfaceView.width,
                            surfaceView.height,
                            Rect(0, 0, 0, 0),
                            surfaceView.display.displayId,
                            destinationFlag,
                            null,
                        )
                    else -> throw e
                }
            }
        }
    }
}
