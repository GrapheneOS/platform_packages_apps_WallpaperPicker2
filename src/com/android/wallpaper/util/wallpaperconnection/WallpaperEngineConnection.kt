package com.android.wallpaper.util.wallpaperconnection

import android.app.Flags.liveWallpaperContentHandling
import android.app.WallpaperColors
import android.app.WallpaperInfo
import android.app.wallpaper.WallpaperDescription
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.service.wallpaper.IWallpaperConnection
import android.service.wallpaper.IWallpaperEngine
import android.service.wallpaper.IWallpaperService
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import com.android.wallpaper.util.WallpaperConnection
import com.android.wallpaper.util.WallpaperConnection.WhichPreview
import java.lang.reflect.InvocationTargetException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

class WallpaperEngineConnection(
    private val displayMetrics: Point,
    private val whichPreview: WhichPreview,
) : IWallpaperConnection.Stub() {

    var engine: IWallpaperEngine? = null

    private var engineContinuation: CancellableContinuation<IWallpaperEngine>? = null
    private var listener: WallpaperEngineConnectionListener? = null

    suspend fun getEngine(
        wallpaperService: IWallpaperService,
        destinationFlag: Int,
        surfaceView: SurfaceView,
        description: WallpaperDescription,
    ): IWallpaperEngine {
        return engine
            ?: suspendCancellableCoroutine { k: CancellableContinuation<IWallpaperEngine> ->
                engineContinuation = k
                attachEngineConnection(
                    wallpaperEngineConnection = this,
                    wallpaperService = wallpaperService,
                    destinationFlag = destinationFlag,
                    surfaceView = surfaceView,
                    description = description,
                )
            }
    }

    override fun attachEngine(engine: IWallpaperEngine?, displayId: Int) {
        // Note that the engine in attachEngine callback is different from the one in engineShown
        // callback, which is called after this attachEngine callback. We use the engine reference
        // passed in attachEngine callback for WallpaperEngineConnection.
        this.engine = engine
        engine?.apply {
            try {
                setVisibility(true)
                resizePreview(Rect(0, 0, displayMetrics.x, displayMetrics.y))
                requestWallpaperColors()
            } catch (e: RemoteException) {
                Log.w(TAG, "Error in attachEngine", e)
            }
        }
    }

    override fun engineShown(engine: IWallpaperEngine?) {
        if (engine != null) {
            dispatchWallpaperCommand(engine)
            // Note that the engines from the attachEngine and engineShown callbacks are different
            // and we only use the reference of engine from the attachEngine callback.
            this.engine?.let { engineContinuation?.resumeWith(Result.success(it)) }
        }
    }

    private fun dispatchWallpaperCommand(engine: IWallpaperEngine) {
        val bundle = Bundle()
        bundle.putInt(WHICH_PREVIEW, whichPreview.value)
        try {
            engine.dispatchWallpaperCommand(COMMAND_PREVIEW_INFO, 0, 0, 0, bundle)
        } catch (e: RemoteException) {
            Log.e(TAG, "Error dispatching wallpaper command: $whichPreview")
        }
    }

    override fun onLocalWallpaperColorsChanged(
        area: RectF?,
        colors: WallpaperColors?,
        displayId: Int,
    ) {
        // Do nothing intended.
    }

    override fun onWallpaperColorsChanged(colors: WallpaperColors?, displayId: Int) {
        listener?.onWallpaperColorsChanged(colors, displayId)
    }

    override fun setWallpaper(name: String?): ParcelFileDescriptor {
        TODO("Not yet implemented")
    }

    fun setListener(listener: WallpaperEngineConnectionListener) {
        this.listener = listener
    }

    fun removeListener() {
        this.listener = null
    }

    companion object {
        const val TAG = "WallpaperEngineConnection"

        const val COMMAND_PREVIEW_INFO = "android.wallpaper.previewinfo"
        const val WHICH_PREVIEW = "which_preview"

        /*
         * Tries to call the attach method used in Android 14(U) and earlier, returning true on
         * success otherwise false.
         */
        private fun tryPreUAttach(
            wallpaperEngineConnection: WallpaperEngineConnection,
            wallpaperService: IWallpaperService,
            destinationFlag: Int,
            surfaceView: SurfaceView,
        ): Boolean {
            try {
                val method =
                    wallpaperService.javaClass.getMethod(
                        "attach",
                        IWallpaperConnection::class.java,
                        IBinder::class.java,
                        Int::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Rect::class.java,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                    )
                method.invoke(
                    wallpaperService,
                    wallpaperEngineConnection,
                    surfaceView.windowToken,
                    WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA,
                    true,
                    surfaceView.width,
                    surfaceView.height,
                    Rect(0, 0, 0, 0),
                    surfaceView.display.displayId,
                    destinationFlag,
                )
                return true
            } catch (e: Exception) {
                when (e) {
                    is NoSuchMethodException,
                    is InvocationTargetException,
                    is IllegalAccessException -> {
                        if (liveWallpaperContentHandling()) {
                            Log.w(
                                TAG,
                                "live wallpaper content handling enabled, but pre-U attach method called",
                            )
                        }
                        return false
                    }

                    else -> throw e
                }
            }
        }

        /*
         * Tries to call the attach method used in Android 16(B) and earlier, returning true on
         * success otherwise false.
         */
        private fun tryPreBAttach(
            wallpaperEngineConnection: WallpaperEngineConnection,
            wallpaperService: IWallpaperService,
            destinationFlag: Int,
            surfaceView: SurfaceView,
        ): Boolean {
            try {
                val method =
                    wallpaperService.javaClass.getMethod(
                        "attach",
                        IWallpaperConnection::class.java,
                        IBinder::class.java,
                        Int::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Rect::class.java,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        WallpaperInfo::class.java,
                    )
                method.invoke(
                    wallpaperService,
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
                return true
            } catch (e: Exception) {
                when (e) {
                    is NoSuchMethodException,
                    is InvocationTargetException,
                    is IllegalAccessException -> {
                        if (liveWallpaperContentHandling()) {
                            Log.w(
                                TAG,
                                "live wallpaper content handling enabled, but pre-B attach method called",
                            )
                        }
                        return false
                    }

                    else -> throw e
                }
            }
        }

        /*
         * This method tries to call historical versions of IWallpaperService#attach since this code
         * may be running against older versions of Android. We have no control over what versions
         * of Android third party users of this code will be running.
         */
        private fun attachEngineConnection(
            wallpaperEngineConnection: WallpaperEngineConnection,
            wallpaperService: IWallpaperService,
            destinationFlag: Int,
            surfaceView: SurfaceView,
            description: WallpaperDescription,
        ) {
            if (
                tryPreUAttach(
                    wallpaperEngineConnection,
                    wallpaperService,
                    destinationFlag,
                    surfaceView,
                )
            ) {
                return
            }
            if (
                tryPreBAttach(
                    wallpaperEngineConnection,
                    wallpaperService,
                    destinationFlag,
                    surfaceView,
                )
            ) {
                return
            }

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
                description,
            )
        }
    }

    /** Interface to be notified of connect/disconnect events from [WallpaperConnection] */
    interface WallpaperEngineConnectionListener {
        /** Called after the wallpaper color is available or updated. */
        fun onWallpaperColorsChanged(colors: WallpaperColors?, displayId: Int) {}
    }
}
