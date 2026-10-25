package com.zts5202.floatingball.data

import android.content.Context

/**
 * 记住用户是否打开悬浮球，以及上次吸附的左右侧和垂直位置。
 * 进程被杀掉之后不会自己拉起服务；下次打开应用时按这里的开关恢复。
 */
class OverlayPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    var anchorRight: Boolean
        get() = prefs.getBoolean(KEY_ANCHOR_RIGHT, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ANCHOR_RIGHT, value).apply()
        }

    /** 0 表示贴在可用区域上沿，1 表示贴在下沿。 */
    var yRatio: Float
        get() = prefs.getFloat(KEY_Y_RATIO, 0.35f).coerceIn(0f, 1f)
        set(value) {
            prefs.edit().putFloat(KEY_Y_RATIO, value.coerceIn(0f, 1f)).apply()
        }

    var askedNotification: Boolean
        get() = prefs.getBoolean(KEY_ASKED_NOTIFICATION, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ASKED_NOTIFICATION, value).apply()
        }

    private companion object {
        const val FILE = "overlay_prefs"
        const val KEY_ENABLED = "enabled"
        const val KEY_ANCHOR_RIGHT = "anchor_right"
        const val KEY_Y_RATIO = "y_ratio"
        const val KEY_ASKED_NOTIFICATION = "asked_notification"
    }
}
