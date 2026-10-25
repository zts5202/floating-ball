package com.zts5202.floatingball.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.zts5202.floatingball.MainActivity
import com.zts5202.floatingball.R
import com.zts5202.floatingball.data.OverlayGate
import com.zts5202.floatingball.data.OverlayPrefs
import com.zts5202.floatingball.permission.OverlayPermission

class FloatingBallService : Service() {
    private lateinit var prefs: OverlayPrefs
    private var window: OverlayWindow? = null
    private var foregroundStarted = false

    private val configCallbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            window?.resnap()
        }

        override fun onLowMemory() = Unit
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        prefs = OverlayPrefs(this)
        registerComponentCallbacks(configCallbacks)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            return START_NOT_STICKY
        }
        startInForeground()
        if (!OverlayGate.shouldShow(prefs.enabled, OverlayPermission.canDraw(this))) {
            shutdown()
            return START_NOT_STICKY
        }
        showWindow()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeWindow()
        unregisterComponentCallbacks(configCallbacks)
        running = false
        super.onDestroy()
    }

    private fun showWindow() {
        if (window != null) return
        val overlay = OverlayWindow(
            context = this,
            windowManager = getSystemService(WindowManager::class.java),
            prefs = prefs,
            onOpenHome = {
                val home = Intent(this, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
                startActivity(home)
            },
            onClose = {
                prefs.enabled = false
                shutdown()
            },
        )
        try {
            overlay.attach()
            window = overlay
        } catch (error: RuntimeException) {
            Log.e(TAG, "无法添加悬浮窗", error)
            shutdown()
        }
    }

    private fun shutdown() {
        removeWindow()
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    private fun removeWindow() {
        val current = window ?: return
        window = null
        try {
            current.detach()
        } catch (error: RuntimeException) {
            Log.w(TAG, "移除悬浮窗失败", error)
        }
    }

    private fun startInForeground() {
        if (foregroundStarted) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ball)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    companion object {
        private const val TAG = "FloatingBallService"
        private const val CHANNEL_ID = "floating_ball"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.zts5202.floatingball.STOP"

        @Volatile
        var running: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, FloatingBallService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            if (!running) return
            val intent = Intent(context, FloatingBallService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
