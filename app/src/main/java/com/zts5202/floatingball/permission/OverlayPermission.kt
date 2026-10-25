package com.zts5202.floatingball.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object OverlayPermission {
    fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun manageIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
    }

    fun notificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
