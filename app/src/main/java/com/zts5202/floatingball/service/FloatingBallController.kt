package com.zts5202.floatingball.service

import android.content.Context
import com.zts5202.floatingball.data.OverlayGate
import com.zts5202.floatingball.data.OverlayPrefs
import com.zts5202.floatingball.permission.OverlayPermission

object FloatingBallController {
    fun sync(context: Context) {
        val appContext = context.applicationContext
        val prefs = OverlayPrefs(appContext)
        if (OverlayGate.shouldShow(prefs.enabled, OverlayPermission.canDraw(appContext))) {
            FloatingBallService.start(appContext)
        } else {
            FloatingBallService.stop(appContext)
        }
    }
}
