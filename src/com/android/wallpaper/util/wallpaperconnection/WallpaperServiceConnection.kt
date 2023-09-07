package com.android.wallpaper.util.wallpaperconnection

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.service.wallpaper.IWallpaperService

class WallpaperServiceConnection(val listener: WallpaperServiceConnectionListener) :
    ServiceConnection {

    override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
        listener.onWallpaperServiceConnected(IWallpaperService.Stub.asInterface(service))
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {
        // TODO b/300979155(giolin): Recycle resources when onServiceDisconnected
        // Do nothing intended
    }

    interface WallpaperServiceConnectionListener {
        fun onWallpaperServiceConnected(wallpaperService: IWallpaperService)
    }
}
