package com.zts5202.floatingball.data

/**
 * 只有用户打开开关，并且系统已经允许「显示在其他应用上层」时，才显示悬浮球。
 */
object OverlayGate {
    fun shouldShow(enabled: Boolean, canDrawOverlays: Boolean): Boolean {
        return enabled && canDrawOverlays
    }
}
