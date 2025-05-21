package com.android.wallpaper.util.wallpaperconnection

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.service.wallpaper.IWallpaperService

class WallpaperServiceConnection(val listener: WallpaperServiceConnectionListener) :
    ServiceConnection {
    var deadConnectionListener: DeadConnectionListener? = null
    var componentName: ComponentName? = null

    override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
        this.componentName = componentName
        listener.onWallpaperServiceConnected(this, IWallpaperService.Stub.asInterface(service))
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {
        deadConnectionListener?.onConnectionDead(this)
    }

    override fun onBindingDied(name: ComponentName?) {
        deadConnectionListener?.onConnectionDead(this)
    }

    interface WallpaperServiceConnectionListener {
        fun onWallpaperServiceConnected(
            serviceConnection: ServiceConnection,
            wallpaperService: IWallpaperService,
        )
    }

    interface DeadConnectionListener {
        fun onConnectionDead(serviceConnection: ServiceConnection)
    }
}
